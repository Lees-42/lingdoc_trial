package com.ruoyi.system.service.lingdoc.ai.dify;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dify Workflow 自动规整业务输出
 * <p>
 * 对应 Dify 响应中 data.outputs 层级的字段结构。
 *
 * @author lingdoc
 */
public class DifyOrganizeOutput
{
    /** 建议保存子路径，如 "工作/求职材料" */
    private String suggestedSubPath;

    /** 分类理由 */
    private String reason;

    /** 分类置信度 / 整体置信度 */
    private BigDecimal confidence;

    /** 标签建议列表 */
    private List<DifyTagOutput> tags;

    /** 建议文件名（含扩展名） */
    private String suggestedName;

    /** 重命名理由 */
    private String renameReason;

    /** 重命名置信度 */
    private BigDecimal renameConfidence;

    /** 内容摘要 */
    private String summary;

    /** 关键词列表 */
    private List<String> keywords;

    /** Token 消耗 */
    private Long tokenCost;

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

    public List<DifyTagOutput> getTags()
    {
        return tags;
    }

    public void setTags(List<DifyTagOutput> tags)
    {
        this.tags = tags;
    }

    public String getSuggestedName()
    {
        return suggestedName;
    }

    public void setSuggestedName(String suggestedName)
    {
        this.suggestedName = suggestedName;
    }

    public String getRenameReason()
    {
        return renameReason;
    }

    public void setRenameReason(String renameReason)
    {
        this.renameReason = renameReason;
    }

    public BigDecimal getRenameConfidence()
    {
        return renameConfidence;
    }

    public void setRenameConfidence(BigDecimal renameConfidence)
    {
        this.renameConfidence = renameConfidence;
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
        return keywords;
    }

    public void setKeywords(List<String> keywords)
    {
        this.keywords = keywords;
    }

    public Long getTokenCost()
    {
        return tokenCost;
    }

    public void setTokenCost(Long tokenCost)
    {
        this.tokenCost = tokenCost;
    }

    /**
     * Dify 标签输出嵌套类
     */
    public static class DifyTagOutput
    {
        /** 标签名 */
        private String tagName;

        /** 标签颜色 Hex，如 #409EFF */
        private String tagColor;

        /** 标签理由 */
        private String reason;

        /** 标签置信度 */
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
}
