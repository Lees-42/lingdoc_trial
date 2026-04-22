package com.ruoyi.system.service.lingdoc.impl;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.ruoyi.framework.datasource.VaultDataSourceManager;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Vault SQLite 数据库初始化器测试
 * 
 * @author lingdoc
 */
@SpringBootTest
public class VaultDbInitializerTest
{
    private static final String TEST_VAULT_PATH = System.getProperty("java.io.tmpdir") + "/lingdoc-test-vault";

    @Autowired
    private VaultDbInitializer vaultDbInitializer;

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
        // 清理 Vault 数据源缓存
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
    public void testInitializeVaultDatabase() throws Exception
    {
        // 执行初始化
        Path dbFile = vaultDbInitializer.initializeVaultDatabase(TEST_VAULT_PATH);

        // 验证目录结构
        assertTrue(Files.exists(Paths.get(TEST_VAULT_PATH, ".lingdoc")), ".lingdoc 目录应已创建");
        assertTrue(Files.exists(dbFile), "vault.db 文件应已创建");

        // 验证数据库可连接且表已创建
        DataSource ds = VaultDataSourceManager.getOrCreateDataSource(TEST_VAULT_PATH);
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement())
        {
            // 验证表存在
            ResultSet rs = stmt.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='lingdoc_file_index'"
            );
            assertTrue(rs.next(), "lingdoc_file_index 表应已创建");
            assertEquals("lingdoc_file_index", rs.getString("name"));

            rs = stmt.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='lingdoc_file_version'"
            );
            assertTrue(rs.next(), "lingdoc_file_version 表应已创建");

            rs = stmt.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='lingdoc_tag'"
            );
            assertTrue(rs.next(), "lingdoc_tag 表应已创建");
        }
    }

    @Test
    public void testIsVaultDatabaseInitialized() throws Exception
    {
        assertFalse(vaultDbInitializer.isVaultDatabaseInitialized(TEST_VAULT_PATH),
                "初始化前应返回 false");

        vaultDbInitializer.initializeVaultDatabase(TEST_VAULT_PATH);

        assertTrue(vaultDbInitializer.isVaultDatabaseInitialized(TEST_VAULT_PATH),
                "初始化后应返回 true");
    }

    @Test
    public void testInitializeVaultDatabase_Idempotent() throws Exception
    {
        // 第一次初始化
        Path dbFile1 = vaultDbInitializer.initializeVaultDatabase(TEST_VAULT_PATH);
        // 第二次初始化（应跳过）
        Path dbFile2 = vaultDbInitializer.initializeVaultDatabase(TEST_VAULT_PATH);

        assertEquals(dbFile1, dbFile2, "重复初始化应返回相同路径");
        assertTrue(Files.exists(dbFile2), "数据库文件应仍然存在");
    }
}
