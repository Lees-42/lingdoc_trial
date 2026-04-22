package com.ruoyi.framework.datasource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Vault SQLite 数据源管理器测试
 * 
 * @author lingdoc
 */
@SpringBootTest
public class VaultDataSourceManagerTest
{
    private static final String TEST_VAULT_PATH = System.getProperty("java.io.tmpdir") + "/lingdoc-test-ds-vault";

    @BeforeEach
    public void setUp() throws Exception
    {
        // 清理测试目录
        Path testPath = Paths.get(TEST_VAULT_PATH);
        if (Files.exists(testPath))
        {
            Files.walk(testPath)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception e) { /* ignore */ }
                });
        }
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        VaultDataSourceManager.removeDataSource(TEST_VAULT_PATH);

        Path testPath = Paths.get(TEST_VAULT_PATH);
        if (Files.exists(testPath))
        {
            Files.walk(testPath)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception e) { /* ignore */ }
                });
        }
    }

    @Test
    public void testGetOrCreateDataSource() throws Exception
    {
        // 首次获取应创建数据库
        DataSource ds = VaultDataSourceManager.getOrCreateDataSource(TEST_VAULT_PATH);
        assertNotNull(ds, "DataSource 不应为 null");

        // 验证可连接
        try (Connection conn = ds.getConnection())
        {
            assertFalse(conn.isClosed(), "连接应处于打开状态");
        }

        // 再次获取应返回缓存的 DataSource
        DataSource ds2 = VaultDataSourceManager.getOrCreateDataSource(TEST_VAULT_PATH);
        assertSame(ds, ds2, "应返回缓存的同一 DataSource 实例");
    }

    @Test
    public void testResolveDataSource() throws Exception
    {
        // 创建数据源
        VaultDataSourceManager.getOrCreateDataSource(TEST_VAULT_PATH);

        // 通过 lookupKey 解析
        String lookupKey = VaultDataSourceManager.VAULT_KEY_PREFIX + TEST_VAULT_PATH;
        DataSource ds = VaultDataSourceManager.resolveDataSource(lookupKey);
        assertNotNull(ds, "应能解析到 DataSource");

        // 无效 key
        assertNull(VaultDataSourceManager.resolveDataSource("INVALID:key"),
                "无效 key 应返回 null");
        assertNull(VaultDataSourceManager.resolveDataSource(null),
                "null key 应返回 null");
    }

    @Test
    public void testWALMode() throws Exception
    {
        DataSource ds = VaultDataSourceManager.getOrCreateDataSource(TEST_VAULT_PATH);

        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement())
        {
            ResultSet rs = stmt.executeQuery("PRAGMA journal_mode");
            assertTrue(rs.next());
            String mode = rs.getString(1);
            assertEquals("wal", mode.toLowerCase(), "应启用 WAL 模式");
        }
    }

    @Test
    public void testRemoveDataSource() throws Exception
    {
        DataSource ds = VaultDataSourceManager.getOrCreateDataSource(TEST_VAULT_PATH);
        assertNotNull(ds);

        VaultDataSourceManager.removeDataSource(TEST_VAULT_PATH);

        // 移除后再次获取应创建新的 DataSource
        DataSource ds2 = VaultDataSourceManager.getOrCreateDataSource(TEST_VAULT_PATH);
        assertNotNull(ds2);
    }

    @Test
    public void testEmptyVaultPath() throws Exception
    {
        assertThrows(IllegalArgumentException.class, () -> {
            VaultDataSourceManager.getOrCreateDataSource("");
        }, "空路径应抛出异常");

        assertThrows(IllegalArgumentException.class, () -> {
            VaultDataSourceManager.getOrCreateDataSource(null);
        }, "null 路径应抛出异常");
    }
}
