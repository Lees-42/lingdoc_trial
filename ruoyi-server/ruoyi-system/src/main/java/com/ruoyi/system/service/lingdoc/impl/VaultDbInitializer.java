package com.ruoyi.system.service.lingdoc.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.sqlite.SQLiteDataSource;
import com.ruoyi.common.config.SQLiteVaultConfig;
import com.ruoyi.common.utils.StringUtils;
import java.nio.charset.StandardCharsets;

/**
 * Vault SQLite 数据库初始化器
 * 
 * 负责在创建新 Vault 时，初始化 `.lingdoc/vault.db` 及表结构。
 * 
 * @author lingdoc
 */
@Component
public class VaultDbInitializer
{
    private static final Logger log = LoggerFactory.getLogger(VaultDbInitializer.class);

    private static final String LINGDOC_DIR = SQLiteVaultConfig.LINGDOC_DIR;
    private static final String DB_FILE = SQLiteVaultConfig.DB_FILE_NAME;
    private static final String DDL_PATH = SQLiteVaultConfig.DDL_PATH;

    /**
     * 初始化指定 Vault 的 SQLite 数据库
     * 
     * @param vaultRoot Vault 根目录绝对路径
     * @return 数据库文件路径
     */
    public Path initializeVaultDatabase(String vaultRoot)
    {
        if (StringUtils.isEmpty(vaultRoot))
        {
            throw new IllegalArgumentException("Vault 根目录不能为空");
        }

        Path rootPath = Paths.get(vaultRoot).toAbsolutePath().normalize();
        Path lingdocDir = rootPath.resolve(LINGDOC_DIR);
        Path dbFile = lingdocDir.resolve(DB_FILE);

        try
        {
            // 创建 .lingdoc 目录
            if (!Files.exists(lingdocDir))
            {
                Files.createDirectories(lingdocDir);
                log.info("创建 Vault 元数据目录: {}", lingdocDir);
            }

            // 如果数据库已存在，跳过初始化
            if (Files.exists(dbFile))
            {
                log.info("Vault SQLite 数据库已存在，跳过初始化: {}", dbFile);
                return dbFile;
            }

            // 创建并初始化数据库
            SQLiteDataSource dataSource = new SQLiteDataSource();
            dataSource.setUrl(SQLiteVaultConfig.buildJdbcUrl(dbFile));
            dataSource.setEncoding(SQLiteVaultConfig.ENCODING);

            executeDdl(dataSource);

            log.info("Vault SQLite 数据库初始化完成: {}", dbFile);
            return dbFile;
        }
        catch (Exception e)
        {
            log.error("初始化 Vault SQLite 数据库失败: {}", dbFile, e);
            throw new RuntimeException("初始化 Vault SQLite 数据库失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行 DDL 脚本初始化表结构
     */
    private void executeDdl(SQLiteDataSource dataSource) throws Exception
    {
        String ddl = loadDdlScript();
        if (StringUtils.isEmpty(ddl))
        {
            throw new RuntimeException("SQLite DDL 脚本加载失败: " + DDL_PATH);
        }

        // 去掉注释后再分割语句
        ddl = SQLiteVaultConfig.cleanSqlScript(ddl);

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement())
        {
            String[] statements = ddl.split(";");
            for (String sql : statements)
            {
                String trimmed = sql.trim();
                if (StringUtils.isEmpty(trimmed))
                {
                    continue;
                }
                // 跳过 PRAGMA
                if (trimmed.toUpperCase().startsWith("PRAGMA"))
                {
                    continue;
                }
                stmt.execute(trimmed);
            }
        }
    }

    /**
     * 加载 DDL 脚本
     */
    private String loadDdlScript() throws Exception
    {
        Resource resource = new ClassPathResource(DDL_PATH);
        if (resource.exists())
        {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        }

        // 开发环境 fallback：从文件系统加载
        Path fallbackPath = Paths.get("ruoyi-server", DDL_PATH);
        if (Files.exists(fallbackPath))
        {
            return Files.readString(fallbackPath, StandardCharsets.UTF_8);
        }

        log.error("无法加载 SQLite DDL 脚本: {}", DDL_PATH);
        return null;
    }

    /**
     * 检查 Vault 数据库是否已初始化
     */
    public boolean isVaultDatabaseInitialized(String vaultRoot)
    {
        if (StringUtils.isEmpty(vaultRoot))
        {
            return false;
        }
        Path dbFile = Paths.get(vaultRoot, LINGDOC_DIR, DB_FILE);
        return Files.exists(dbFile);
    }

    /**
     * 获取 Vault 数据库文件路径
     */
    public Path getVaultDatabasePath(String vaultRoot)
    {
        return Paths.get(vaultRoot, LINGDOC_DIR, DB_FILE).toAbsolutePath().normalize();
    }
}
