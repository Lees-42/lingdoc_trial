package com.ruoyi.common.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vault 上下文持有者
 * 
 * 通过 ThreadLocal 维护当前线程操作的 Vault 路径，
 * 供 DynamicDataSource 路由到对应的 SQLite 数据源。
 * 
 * @author lingdoc
 */
public class VaultContextHolder
{
    public static final Logger log = LoggerFactory.getLogger(VaultContextHolder.class);

    /**
     * 当前 Vault 路径（绝对路径）
     */
    private static final ThreadLocal<String> VAULT_PATH_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前 Vault 路径
     */
    public static void setCurrentVaultPath(String vaultPath)
    {
        log.debug("设置当前 Vault 路径: {}", vaultPath);
        VAULT_PATH_HOLDER.set(vaultPath);
    }

    /**
     * 获取当前 Vault 路径
     */
    public static String getCurrentVaultPath()
    {
        return VAULT_PATH_HOLDER.get();
    }

    /**
     * 清空当前 Vault 路径
     */
    public static void clearCurrentVaultPath()
    {
        VAULT_PATH_HOLDER.remove();
    }

    /**
     * 判断是否处于 Vault 上下文
     */
    public static boolean isInVaultContext()
    {
        return VAULT_PATH_HOLDER.get() != null;
    }
}
