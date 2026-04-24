package com.ruoyi.framework.datasource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import org.sqlite.SQLiteDataSource;
import com.ruoyi.common.config.SQLiteVaultConfig;
import com.ruoyi.common.utils.StringUtils;
import java.nio.charset.StandardCharsets;

/**
 * Vault SQLite 数据源管理器
 * 
 * 负责按需创建、缓存和管理每个 Vault 对应的 SQLite DataSource。
 * 使用懒加载模式：首次访问某 Vault 时自动创建 SQLite 数据库文件和表结构。
 * 
 * @author lingdoc
 */
public class VaultDataSourceManager
{
    private static final Logger log = LoggerFactory.getLogger(VaultDataSourceManager.class);

    /**
     * Vault 数据源缓存：vaultPath → DataSource
     */
    private static final Map<String, DataSource> VAULT_DATA_SOURCE_MAP = new ConcurrentHashMap<>();

    /**
     * SQLite DDL 脚本缓存
     */
    private static volatile String sqliteDdlScript = null;

    /**
     * 数据源 key 前缀
     */
    public static final String VAULT_KEY_PREFIX = SQLiteVaultConfig.VAULT_KEY_PREFIX;

    /**
     * 获取或创建指定 Vault 的 DataSource
     * 
     * @param vaultPath Vault 根目录绝对路径
     * @return SQLite DataSource
     */
    public static DataSource getOrCreateDataSource(String vaultPath)
    {
        if (StringUtils.isEmpty(vaultPath))
        {
            throw new IllegalArgumentException("Vault 路径不能为空");
        }

        // 标准化路径作为 key
        String normalizedPath = Paths.get(vaultPath).toAbsolutePath().normalize().toString();
        String cacheKey = VAULT_KEY_PREFIX + normalizedPath;

        DataSource dataSource = VAULT_DATA_SOURCE_MAP.get(cacheKey);
        if (dataSource != null)
        {
            return dataSource;
        }

        synchronized (VAULT_DATA_SOURCE_MAP)
        {
            // 双重检查
            dataSource = VAULT_DATA_SOURCE_MAP.get(cacheKey);
            if (dataSource != null)
            {
                return dataSource;
            }

            dataSource = createSQLiteDataSource(normalizedPath);
            VAULT_DATA_SOURCE_MAP.put(cacheKey, dataSource);
            log.info("创建并缓存 Vault SQLite 数据源: {}", normalizedPath);
            return dataSource;
        }
    }

    /**
     * 根据 lookupKey 获取 DataSource（供 DynamicDataSource 使用）
     */
    public static DataSource resolveDataSource(String lookupKey)
    {
        if (StringUtils.isEmpty(lookupKey) || !lookupKey.startsWith(VAULT_KEY_PREFIX))
        {
            return null;
        }
        String vaultPath = lookupKey.substring(VAULT_KEY_PREFIX.length());
        return getOrCreateDataSource(vaultPath);
    }

    /**
     * 创建 SQLite DataSource
     */
    private static DataSource createSQLiteDataSource(String vaultPath)
    {
        Path lingdocDir = SQLiteVaultConfig.resolveLingdocDir(vaultPath);
        Path dbFile = SQLiteVaultConfig.resolveDbPath(vaultPath);
        boolean dbExists = Files.exists(dbFile);

        // 如果目录不存在，先创建
        if (!Files.exists(lingdocDir))
        {
            try
            {
                Files.createDirectories(lingdocDir);
                log.info("创建 Vault 元数据目录: {}", lingdocDir);
            }
            catch (Exception e)
            {
                log.error("创建 Vault 元数据目录失败: {}", lingdocDir, e);
                throw new RuntimeException("创建 Vault 元数据目录失败: " + e.getMessage(), e);
            }
        }

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(SQLiteVaultConfig.buildJdbcUrl(dbFile));
        dataSource.setEncoding(SQLiteVaultConfig.ENCODING);

        // 如果数据库文件不存在，初始化表结构
        if (!dbExists)
        {
            try
            {
                initializeDatabase(dataSource);
            }
            catch (Exception e)
            {
                log.error("初始化 Vault SQLite 数据库失败: {}", dbFile, e);
                throw new RuntimeException("初始化 Vault SQLite 数据库失败: " + e.getMessage(), e);
            }
        }

        return dataSource;
    }

    /**
     * 初始化 SQLite 数据库表结构
     */
    private static void initializeDatabase(SQLiteDataSource dataSource) throws Exception
    {
        String ddl = loadDdlScript();
        if (StringUtils.isEmpty(ddl))
        {
            throw new RuntimeException("SQLite DDL 脚本加载失败");
        }

        // 去掉注释后再分割语句
        ddl = SQLiteVaultConfig.cleanSqlScript(ddl);

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement())
        {
            // 按分号分割，逐条执行
            String[] statements = ddl.split(";");
            for (String sql : statements)
            {
                String trimmed = sql.trim();
                if (StringUtils.isEmpty(trimmed))
                {
                    continue;
                }
                // 跳过 PRAGMA（已在连接 URL 中设置）
                if (trimmed.toUpperCase().startsWith("PRAGMA"))
                {
                    continue;
                }
                log.debug("执行 SQL: {}", trimmed.substring(0, Math.min(trimmed.length(), 100)));
                stmt.execute(trimmed);
            }
            log.info("Vault SQLite 数据库初始化完成");
        }

        // 初始化后验证关键表是否存在
        verifyTableStructure(dataSource);
    }

    /**
     * 验证关键表结构是否正确存在
     * 防止因初始化脚本问题导致表缺失
     */
    private static void verifyTableStructure(SQLiteDataSource dataSource) throws Exception
    {
        String[] requiredTables = { "lingdoc_file_index", "lingdoc_tag_binding", "lingdoc_tag" };

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement())
        {
            for (String tableName : requiredTables)
            {
                // SQLite 检查表存在性
                String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'";
                if (!stmt.executeQuery(sql).next())
                {
                    String errorMsg = "Vault 数据库缺少必要表: " + tableName;
                    log.error(errorMsg);
                    throw new RuntimeException(errorMsg);
                }
                else
                {
                    log.debug("验证表存在: {}", tableName);
                }
            }
            log.info("Vault SQLite 表结构验证成功");
        }
    }



    /**
     * 加载 SQLite DDL 脚本
     */
    private static String loadDdlScript() throws Exception
    {
        if (sqliteDdlScript != null)
        {
            return sqliteDdlScript;
        }

        Resource resource = new ClassPathResource(SQLiteVaultConfig.DDL_PATH);
        if (!resource.exists())
        {
            // 尝试从文件系统加载（开发调试）
            resource = new org.springframework.core.io.FileSystemResource("ruoyi-server/" + SQLiteVaultConfig.DDL_PATH);
        }

        if (resource.exists())
        {
            sqliteDdlScript = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return sqliteDdlScript;
        }

        log.error("无法加载 SQLite DDL 脚本: {}", SQLiteVaultConfig.DDL_PATH);
        return null;
    }

    /**
     * 移除指定 Vault 的数据源缓存（用于删除 vault 时清理）
     */
    public static void removeDataSource(String vaultPath)
    {
        if (StringUtils.isEmpty(vaultPath))
        {
            return;
        }
        String normalizedPath = Paths.get(vaultPath).toAbsolutePath().normalize().toString();
        String cacheKey = VAULT_KEY_PREFIX + normalizedPath;
        VAULT_DATA_SOURCE_MAP.remove(cacheKey);
        log.info("移除 Vault SQLite 数据源缓存: {}", normalizedPath);
    }

    /**
     * 清空所有 Vault 数据源缓存
     */
    public static void clearAll()
    {
        VAULT_DATA_SOURCE_MAP.clear();
        log.info("清空所有 Vault SQLite 数据源缓存");
    }
}
