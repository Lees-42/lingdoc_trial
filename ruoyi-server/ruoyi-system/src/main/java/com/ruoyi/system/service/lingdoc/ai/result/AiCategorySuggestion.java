package com.ruoyi.system.service.lingdoc.ai.result;

import java.math.BigDecimal;

/**
 * AI 分类建议
 * 
 * @author lingdoc
 */
public class AiCategorySuggestion
{
    /** 建议子路径，如 "工作/合同/2024" */
    private String suggestedSubPath;

    /** 建议理由 */
    private String reason;

    /** 置信度 */
    private BigDecimal confidence;

    public String getSuggestedSubPath()
    {
        return suggestedSubPath;
    }

    public void setSuggestedSubPath(String suggestedSubPath)
    {
        this.suggestedSubPath = suggestedSubPath;
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
