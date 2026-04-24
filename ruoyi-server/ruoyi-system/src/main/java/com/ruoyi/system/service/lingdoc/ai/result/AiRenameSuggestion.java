package com.ruoyi.system.service.lingdoc.ai.result;

import java.math.BigDecimal;

/**
 * AI 重命名建议
 * 
 * @author lingdoc
 */
public class AiRenameSuggestion
{
    /** 建议文件名（含扩展名） */
    private String suggestedName;

    /** 建议理由 */
    private String reason;

    /** 置信度 */
    private BigDecimal confidence;

    public String getSuggestedName()
    {
        return suggestedName;
    }

    public void setSuggestedName(String suggestedName)
    {
        this.suggestedName = suggestedName;
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
