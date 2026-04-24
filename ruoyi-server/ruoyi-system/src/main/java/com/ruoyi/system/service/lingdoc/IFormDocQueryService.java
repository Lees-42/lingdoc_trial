package com.ruoyi.system.service.lingdoc;

import com.ruoyi.system.domain.lingdoc.ai.FormDocQueryRequest;
import com.ruoyi.system.domain.lingdoc.ai.FormDocQueryResponse;

/**
 * 表格填写助手 - 文档查询服务接口
 * <p>
 * 根据表格字段从 Vault 中检索最相关的参考文档。
 * 被 VaultDataSourceAspect 拦截，自动切换至 Vault 对应的 SQLite 数据源。
 *
 * @author lingdoc
 */
public interface IFormDocQueryService
{
    /**
     * 根据字段列表查询相关参考文档
     *
     * @param request 查询请求
     * @return 查询响应
     */
    FormDocQueryResponse queryDocs(FormDocQueryRequest request);
}
