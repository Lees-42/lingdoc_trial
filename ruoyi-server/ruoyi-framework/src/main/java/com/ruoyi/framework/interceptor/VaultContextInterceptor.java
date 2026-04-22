package com.ruoyi.framework.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.datasource.VaultContextHolder;
import com.ruoyi.system.domain.lingdoc.LingdocUserRepo;
import com.ruoyi.system.service.lingdoc.ILingdocUserRepoService;

/**
 * Vault 上下文拦截器
 * 
 * 从 HTTP 请求头中读取 X-Vault-Path，设置当前线程的 Vault 上下文，
 * 供后续 Service 层动态数据源切换使用。
 * 
 * 配置在 WebMvcConfig 中，拦截 /lingdoc/** 路径。
 * 
 * @author lingdoc
 */
@Component
public class VaultContextInterceptor implements HandlerInterceptor
{
    private static final Logger log = LoggerFactory.getLogger(VaultContextInterceptor.class);

    /**
     * Vault 路径请求头名称
     */
    public static final String HEADER_VAULT_PATH = "X-Vault-Path";

    @Autowired
    private ILingdocUserRepoService userRepoService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        String vaultPath = request.getHeader(HEADER_VAULT_PATH);
        if (StringUtils.isEmpty(vaultPath))
        {
            // 尝试获取当前用户的默认仓库路径
            try
            {
                Long userId = SecurityUtils.getUserId();
                LingdocUserRepo repo = userRepoService.getDefaultUserRepo(userId);
                if (repo != null)
                {
                    vaultPath = repo.getRepoPath();
                    log.debug("[VaultContextInterceptor] 自动回退到默认 Vault: {}", vaultPath);
                }
            }
            catch (Exception e)
            {
                log.debug("[VaultContextInterceptor] 无法获取默认 Vault 路径: {}", e.getMessage());
            }
        }
        if (StringUtils.isNotEmpty(vaultPath))
        {
            VaultContextHolder.setCurrentVaultPath(vaultPath);
            log.debug("[VaultContextInterceptor] 设置 Vault 路径: {}", vaultPath);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception
    {
        // 请求完成后清理 Vault 上下文，防止线程池复用导致数据污染
        VaultContextHolder.clearCurrentVaultPath();
        log.debug("[VaultContextInterceptor] 清理 Vault 上下文");
    }
}
