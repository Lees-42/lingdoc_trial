package com.ruoyi.framework.datasource;

import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import com.ruoyi.common.config.SQLiteVaultConfig;
import com.ruoyi.common.utils.StringUtils;

/**
 * 动态数据源
 * 
 * 支持 MASTER/SLAVE 预定义数据源切换，以及运行时 Vault SQLite 数据源动态路由。
 * 
 * @author ruoyi
 * @author lingdoc
 */
public class DynamicDataSource extends AbstractRoutingDataSource
{
    private static final Logger log = LoggerFactory.getLogger(DynamicDataSource.class);

    public DynamicDataSource(DataSource defaultTargetDataSource, Map<Object, Object> targetDataSources)
    {
        super.setDefaultTargetDataSource(defaultTargetDataSource);
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey()
    {
        return DynamicDataSourceContextHolder.getDataSourceType();
    }

    /**
     * 重写目标数据源解析逻辑，支持运行时 Vault SQLite 数据源。
     * 当 resolvedDataSources 中找不到对应 key 时，尝试通过 VaultDataSourceManager 解析。
     */
    @Override
    protected DataSource determineTargetDataSource()
    {
        Object lookupKey = determineCurrentLookupKey();
        if (lookupKey == null)
        {
            return super.determineTargetDataSource();
        }

        String keyStr = lookupKey.toString();
        if (StringUtils.isNotEmpty(keyStr) && keyStr.startsWith(SQLiteVaultConfig.VAULT_KEY_PREFIX))
        {
            DataSource vaultDs = VaultDataSourceManager.resolveDataSource(keyStr);
            if (vaultDs != null)
            {
                return vaultDs;
            }
            log.warn("Vault 数据源解析失败，回退到默认数据源: {}", keyStr);
        }

        return super.determineTargetDataSource();
    }
}