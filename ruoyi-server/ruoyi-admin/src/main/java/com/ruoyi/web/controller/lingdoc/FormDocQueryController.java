package com.ruoyi.web.controller.lingdoc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.lingdoc.ai.FormDocQueryRequest;
import com.ruoyi.system.domain.lingdoc.ai.FormDocQueryResponse;
import com.ruoyi.system.service.lingdoc.IFormDocQueryService;

/**
 * 表格填写助手 - 文档查询接口
 * <p>
 * 供 Dify Workflow HTTP Request 节点调用，根据表格字段从 Vault 检索相关参考文档。
 * <p>
 * 请求路径：POST /lingdoc/ai/form/query-docs
 * 鉴权：Spring Security JWT（Dify 需在 Header 中传入 Authorization: Bearer {token}）
 * Vault 上下文：通过 X-Vault-Path Header 传递，VaultContextInterceptor 自动解析
 *
 * @author lingdoc
 */
@RestController
@RequestMapping("/lingdoc/ai/form")
public class FormDocQueryController
{
    private static final Logger log = LoggerFactory.getLogger(FormDocQueryController.class);

    @Autowired
    private IFormDocQueryService formDocQueryService;

    /** 内部 API Key（从配置文件读取，专供 Dify 等内部服务调用） */
    @Value("${lingdoc.ai.dify.internal-api-key:}")
    private String internalApiKey;

    /**
     * 根据字段列表查询相关参考文档
     * <p>
     * 支持两种鉴权方式：
     * 1. Spring Security JWT（前端用户正常调用，走过滤器链）
     * 2. 内部 API Key（Dify Workflow HTTP Request 节点调用）
     *
     * @param request 查询请求（fieldNames, tableType, maxDocs, maxCharsPerDoc）
     * @param apiKey  内部 API Key（Header: X-API-Key）
     * @return AjaxResult { code: 200, msg: "success", data: { docs, totalMatched, queryTimeMs } }
     */
    @PostMapping("/query-docs")
    public AjaxResult queryDocs(
            @RequestBody FormDocQueryRequest request,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey)
    {
        // 鉴权校验：支持 JWT（Spring Security 已处理）或内部 API Key
        if (!isAuthorized(apiKey))
        {
            return AjaxResult.error(401, "未授权访问，请提供有效的 X-API-Key 或登录凭证");
        }

        if (request == null || request.getFieldNames() == null || request.getFieldNames().isEmpty())
        {
            return AjaxResult.error("fieldNames 不能为空");
        }

        log.debug("收到文档查询请求, fieldNames={}, tableType={}, maxDocs={}",
                request.getFieldNames(), request.getTableType(), request.getMaxDocs());

        FormDocQueryResponse response = formDocQueryService.queryDocs(request);
        return AjaxResult.success(response);
    }

    /**
     * 鉴权校验
     * <p>
     * 优先级：
     * 1. 如果 Spring Security 过滤器已通过（当前用户已登录），直接放行
     * 2. 如果提供了有效的 X-API-Key，也放行（供 Dify 等内部服务使用）
     */
    private boolean isAuthorized(String apiKey)
    {
        // 方案 A：检查是否已通过 Spring Security 认证（用户已登录）
        try
        {
            // 如果能获取到当前登录用户，说明 JWT 鉴权已通过
            com.ruoyi.common.utils.SecurityUtils.getLoginUser();
            return true;
        }
        catch (Exception e)
        {
            // JWT 未通过，继续检查 API Key
        }

        // 方案 B：API Key 校验（Dify 内部调用）
        if (StringUtils.isNotEmpty(internalApiKey) && internalApiKey.equals(apiKey))
        {
            log.debug("API Key 鉴权通过");
            return true;
        }

        log.warn("鉴权失败，未提供有效的 JWT 或 API Key");
        return false;
    }
}
