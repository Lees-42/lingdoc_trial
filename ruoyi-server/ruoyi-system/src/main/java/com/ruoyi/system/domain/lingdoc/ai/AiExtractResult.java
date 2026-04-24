package com.ruoyi.system.domain.lingdoc.ai;

import java.util.List;

/**
 * AI 字段识别返回结果
 * 
 * @author lingdoc
 */
public class AiExtractResult
{
    /** 识别出的字段列表 */
    private List<AiField> fields;

    /** 参考文档列表 */
    private List<AiReference> references;

    /** Token 消耗量 */
    private Integer tokenCost;

    public List<AiField> getFields()
    {
        return fields;
    }

    public void setFields(List<AiField> fields)
    {
        this.fields = fields;
    }

    public List<AiReference> getReferences()
    {
        return references;
    }

    public void setReferences(List<AiReference> references)
    {
        this.references = references;
    }

    public Integer getTokenCost()
    {
        return tokenCost;
    }

    public void setTokenCost(Integer tokenCost)
    {
        this.tokenCost = tokenCost;
    }
}
