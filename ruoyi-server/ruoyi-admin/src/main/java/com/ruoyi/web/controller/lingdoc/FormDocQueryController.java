package com.ruoyi.web.controller.lingdoc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
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

    /**
     * 根据字段列表查询相关参考文档
     *
     * @param request 查询请求（fieldNames, tableType, maxDocs, maxCharsPerDoc）
     * @return AjaxResult { code: 200, msg: "success", data: { docs, totalMatched, queryTimeMs } }
     */
    @PostMapping("/query-docs")
    public AjaxResult queryDocs(@RequestBody FormDocQueryRequest request)
    {
        if (request == null || request.getFieldNames() == null || request.getFieldNames().isEmpty())
        {
            return AjaxResult.error("fieldNames 不能为空");
        }

        log.debug("收到文档查询请求, fieldNames={}, tableType={}, maxDocs={}",
                request.getFieldNames(), request.getTableType(), request.getMaxDocs());

        FormDocQueryResponse response = formDocQueryService.queryDocs(request);
        return AjaxResult.success(response);
    }
}
