package com.ruoyi.common.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SQLite Vault 配置常量
 *
 * 集中管理所有与 Vault SQLite 数据库相关的配置项，避免硬编码散布在多个模块中。
 * 纯常量工具类，无 Spring 依赖，可被任意模块直接使用。
 *
 * @author lingdoc
 */
public final class SQLiteVaultConfig
{
    private SQLiteVaultConfig()
    {
    }

    /** Vault 元数据目录名（位于仓库根目录下） */
    public static final String LINGDOC_DIR = ".lingdoc";

    /** SQLite 数据库文件名 */
    public static final String DB_FILE_NAME = "vault.db";

    /** JDBC URL 前缀 */
    public static final String JDBC_PREFIX = "jdbc:sqlite:";

    /** SQLite 连接参数 */
    public static final String JDBC_PARAMS = "journal_mode=WAL&busy_timeout=5000";

    /** 数据库编码 */
    public static final String ENCODING = "UTF-8";

    /** DDL 脚本类路径 */
    public static final String DDL_PATH = "sql/13-vault-sqlite.sql";

    /** Vault 数据源 lookup key 前缀 */
    public static final String VAULT_KEY_PREFIX = "VAULT:";

    /**
     * 构建完整 JDBC URL
     *
     * @param dbFile 数据库文件路径
     * @return 完整 JDBC URL，如 jdbc:sqlite:/path/to/vault.db?journal_mode=WAL&busy_timeout=5000
     */
    public static String buildJdbcUrl(Path dbFile)
    {
        return JDBC_PREFIX + dbFile.toString() + "?" + JDBC_PARAMS;
    }

    /**
     * 解析 Vault 根目录下的 .lingdoc 元数据目录路径
     *
     * @param vaultRoot Vault 根目录绝对路径
     * @return .lingdoc 目录路径
     */
    public static Path resolveLingdocDir(String vaultRoot)
    {
        return Paths.get(vaultRoot, LINGDOC_DIR).toAbsolutePath().normalize();
    }

    /**
     * 解析 Vault 根目录下的数据库文件路径
     *
     * @param vaultRoot Vault 根目录绝对路径
     * @return vault.db 文件路径
     */
    public static Path resolveDbPath(String vaultRoot)
    {
        return resolveLingdocDir(vaultRoot).resolve(DB_FILE_NAME);
    }

    /**
     * 去掉 SQL 脚本中的注释（多行 /* ... * / 和单行 -- ...）
     *
     * @param sql 原始 SQL 脚本
     * @return 去除注释后的 SQL
     */
    public static String cleanSqlScript(String sql)
    {
        if (sql == null)
        {
            return null;
        }
        // 去掉多行注释 /* ... */
        sql = sql.replaceAll("(?s)/\\*.*?\\*/", "");
        // 去掉单行注释 -- ...
        sql = sql.replaceAll("(?m)^\\s*--.*$", "");
        return sql;
    }
}
