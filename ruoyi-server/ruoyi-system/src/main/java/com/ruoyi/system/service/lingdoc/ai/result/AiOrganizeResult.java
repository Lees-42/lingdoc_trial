package com.ruoyi.system.service.lingdoc.ai.result;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 自动规整结果
 * 
 * @author lingdoc
 */
public class AiOrganizeResult
{
    /** 分类建议 */
    private AiCategorySuggestion category;

    /** 标签建议列表 */
    private List<AiTagSuggestion> tags;

    /** 重命名建议 */
    private AiRenameSuggestion rename;

    /** 内容摘要 */
    private String summary;

    /** 关键词列表 */
    private List<String> keywords;

    /** 整体置信度 */
    private BigDecimal confidence;

    /** Token 消耗 */
    private Long tokenCost;

    public AiCategorySuggestion getCategory()
    {
        return category;
    }

    public void setCategory(AiCategorySuggestion category)
    {
        this.category = category;
    }

    public List<AiTagSuggestion> getTags()
    {
        if (tags == null)
        {
            tags = new ArrayList<>();
        }
        return tags;
    }

    public void setTags(List<AiTagSuggestion> tags)
    {
        this.tags = tags;
    }

    public AiRenameSuggestion getRename()
    {
        return rename;
    }

    public void setRename(AiRenameSuggestion rename)
    {
        this.rename = rename;
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    public List<String> getKeywords()
    {
        if (keywords == null)
        {
            keywords = new ArrayList<>();
        }
        return keywords;
    }

    public void setKeywords(List<String> keywords)
    {
        this.keywords = keywords;
    }

    public BigDecimal getConfidence()
    {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence)
    {
        this.confidence = confidence;
    }

    public Long getTokenCost()
    {
        return tokenCost;
    }

    public void setTokenCost(Long tokenCost)
    {
        this.tokenCost = tokenCost;
    }
}
