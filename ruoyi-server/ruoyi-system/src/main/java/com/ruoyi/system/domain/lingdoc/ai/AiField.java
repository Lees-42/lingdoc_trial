package com.ruoyi.system.domain.lingdoc.ai;

import java.math.BigDecimal;

/**
 * AI 识别的字段定义
 * 
 * @author lingdoc
 */
public class AiField
{
    /** 字段名称（如：姓名） */
    private String fieldName;

    /** 字段类型（text/date/number/select/checkbox） */
    private String fieldType;

    /** 字段在文档中的原始标签文本（如：姓名（中文）） */
    private String fieldLabel;

    /** AI 建议的填写值 */
    private String suggestedValue;

    /** AI 置信度（0.00~1.00） */
    private BigDecimal confidence;

    /** 值来源的 Vault 文档ID */
    private String sourceDocId;

    /** 来源文档名称 */
    private String sourceDocName;

    /** 字段排序号 */
    private Integer sortOrder;

    public String getFieldName()
    {
        return fieldName;
    }

    public void setFieldName(String fieldName)
    {
        this.fieldName = fieldName;
    }

    public String getFieldType()
    {
        return fieldType;
    }

    public void setFieldType(String fieldType)
    {
        this.fieldType = fieldType;
    }

    public String getFieldLabel()
    {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel)
    {
        this.fieldLabel = fieldLabel;
    }

    public String getSuggestedValue()
    {
        return suggestedValue;
    }

    public void setSuggestedValue(String suggestedValue)
    {
        this.suggestedValue = suggestedValue;
    }

    public BigDecimal getConfidence()
    {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence)
    {
        this.confidence = confidence;
    }

    public String getSourceDocId()
    {
        return sourceDocId;
    }

    public void setSourceDocId(String sourceDocId)
    {
        this.sourceDocId = sourceDocId;
    }

    public String getSourceDocName()
    {
        return sourceDocName;
    }

    public void setSourceDocName(String sourceDocName)
    {
        this.sourceDocName = sourceDocName;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }
}
