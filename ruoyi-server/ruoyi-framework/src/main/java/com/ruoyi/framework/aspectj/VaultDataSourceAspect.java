package com.ruoyi.framework.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.datasource.DynamicDataSourceContextHolder;
import com.ruoyi.common.datasource.VaultContextHolder;
import com.ruoyi.common.config.SQLiteVaultConfig;
import com.ruoyi.framework.datasource.VaultDataSourceManager;

/**
 * Vault 数据源切换 AOP
 * 
 * 拦截 lingdoc 业务层方法，若当前处于 Vault 上下文，
 * 则自动切换 DynamicDataSource 到对应的 SQLite 数据源。
 * 
 * 执行顺序：在 DataSourceAspect 之前执行（Order 值更小），
 * 确保 Vault 数据源优先级高于注解指定的数据源。
 * 
 * @author lingdoc
 */
@Aspect
@Order(0)
@Component
public class VaultDataSourceAspect
{
    protected Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 拦截 lingdoc 业务服务层方法（排除用户仓库配置服务，因其操作中心 MySQL）
     */
    @Pointcut("execution(* com.ruoyi.system.service.lingdoc.ILingdocVaultService.*(..)) || "
            + "execution(* com.ruoyi.system.service.lingdoc.ILingdocTagService.*(..)) || "
            + "execution(* com.ruoyi.system.service.lingdoc.ILingdocFormTaskService.*(..)) || "
            + "execution(* com.ruoyi.system.service.lingdoc.ILingdocInboxService.*(..)) || "
            + "execution(* com.ruoyi.system.service.lingdoc.LingdocVaultServiceImpl.*(..)) || "
            + "execution(* com.ruoyi.system.service.lingdoc.LingdocTagServiceImpl.*(..)) || "
            + "execution(* com.ruoyi.system.service.lingdoc.LingdocFormTaskServiceImpl.*(..)) || "
            + "execution(* com.ruoyi.system.service.lingdoc.LingdocInboxServiceImpl.*(..))")
    public void lingdocServicePointCut()
    {
    }

    @Around("lingdocServicePointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable
    {
        String vaultPath = VaultContextHolder.getCurrentVaultPath();

        if (StringUtils.isEmpty(vaultPath))
        {
            throw new ServiceException("请先创建仓库");
        }

        String previousDs = DynamicDataSourceContextHolder.getDataSourceType();
        String lookupKey = SQLiteVaultConfig.VAULT_KEY_PREFIX + vaultPath;
        DynamicDataSourceContextHolder.setDataSourceType(lookupKey);
        logger.debug("[VaultDataSourceAspect] 切换数据源: {}, 原数据源: {}", lookupKey, previousDs);

        try
        {
            return point.proceed();
        }
        finally
        {
            if (previousDs != null)
            {
                DynamicDataSourceContextHolder.setDataSourceType(previousDs);
                logger.debug("[VaultDataSourceAspect] 恢复数据源: {}", previousDs);
            }
            else
            {
                DynamicDataSourceContextHolder.clearDataSourceType();
                logger.debug("[VaultDataSourceAspect] 清理 Vault 数据源上下文");
            }
        }
    }
}
