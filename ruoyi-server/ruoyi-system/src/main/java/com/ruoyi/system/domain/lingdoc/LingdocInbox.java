package com.ruoyi.system.domain.lingdoc;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 收件箱文件表 lingdoc_inbox
 * 
 * @author lingdoc
 */
public class LingdocInbox extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 收件箱记录ID */
    private String inboxId;

    /** 所属用户ID */
    private Long userId;

    /** 原始文件名 */
    private String originalName;

    /** 文件类型 */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件绝对路径 */
    private String absPath;

    /** 状态：uploaded/organizing/pending/confirmed/failed */
    private String status;

    /** AI建议文件名 */
    private String suggestedName;

    /** AI建议保存路径 */
    private String suggestedPath;

    /** 建议标签ID（逗号分隔） */
    private String tagIds;

    /** AI摘要 */
    private String aiSummary;

    /** AI关键词（逗号分隔） */
    private String aiKeywords;

    /** AI置信度 */
    private BigDecimal confidence;

    /** Token消耗 */
    private Integer tokenCost;

    /** 错误信息 */
    private String errorMsg;

    public String getInboxId()
    {
        return inboxId;
    }

    public void setInboxId(String inboxId)
    {
        this.inboxId = inboxId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getOriginalName()
    {
        return originalName;
    }

    public void setOriginalName(String originalName)
    {
        this.originalName = originalName;
    }

    public String getFileType()
    {
        return fileType;
    }

    public void setFileType(String fileType)
    {
        this.fileType = fileType;
    }

    public Long getFileSize()
    {
        return fileSize;
    }

    public void setFileSize(Long fileSize)
    {
        this.fileSize = fileSize;
    }

    public String getAbsPath()
    {
        return absPath;
    }

    public void setAbsPath(String absPath)
    {
        this.absPath = absPath;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getSuggestedName()
    {
        return suggestedName;
    }

    public void setSuggestedName(String suggestedName)
    {
        this.suggestedName = suggestedName;
    }

    public String getSuggestedPath()
    {
        return suggestedPath;
    }

    public void setSuggestedPath(String suggestedPath)
    {
        this.suggestedPath = suggestedPath;
    }

    public String getTagIds()
    {
        return tagIds;
    }

    public void setTagIds(String tagIds)
    {
        this.tagIds = tagIds;
    }

    public String getAiSummary()
    {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary)
    {
        this.aiSummary = aiSummary;
    }

    public String getAiKeywords()
    {
        return aiKeywords;
    }

    public void setAiKeywords(String aiKeywords)
    {
        this.aiKeywords = aiKeywords;
    }

    public BigDecimal getConfidence()
    {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence)
    {
        this.confidence = confidence;
    }

    public Integer getTokenCost()
    {
        return tokenCost;
    }

    public void setTokenCost(Integer tokenCost)
    {
        this.tokenCost = tokenCost;
    }

    public String getErrorMsg()
    {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg)
    {
        this.errorMsg = errorMsg;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("inboxId", getInboxId())
            .append("userId", getUserId())
            .append("originalName", getOriginalName())
            .append("fileType", getFileType())
            .append("fileSize", getFileSize())
            .append("absPath", getAbsPath())
            .append("status", getStatus())
            .append("suggestedName", getSuggestedName())
            .append("suggestedPath", getSuggestedPath())
            .append("tagIds", getTagIds())
            .append("aiSummary", getAiSummary())
            .append("aiKeywords", getAiKeywords())
            .append("confidence", getConfidence())
            .append("tokenCost", getTokenCost())
            .append("errorMsg", getErrorMsg())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
