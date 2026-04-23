package com.ruoyi.system.service.lingdoc.ai.result;

import java.math.BigDecimal;

/**
 * AI 标签建议
 * 
 * @author lingdoc
 */
public class AiTagSuggestion
{
    /** 建议标签名 */
    private String tagName;

    /** 建议颜色 */
    private String tagColor;

    /** 建议理由 */
    private String reason;

    /** 置信度 */
    private BigDecimal confidence;

    public String getTagName()
    {
        return tagName;
    }

    public void setTagName(String tagName)
    {
        this.tagName = tagName;
    }

    public String getTagColor()
    {
        return tagColor;
    }

    public void setTagColor(String tagColor)
    {
        this.tagColor = tagColor;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public BigDecimal getConfidence()
    {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence)
    {
        this.confidence = confidence;
    }
}
