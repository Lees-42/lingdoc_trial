package com.ruoyi.system.domain.lingdoc;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 文件AI元数据表（脚手架） lingdoc_file_ai_meta
 * 
 * @author lingdoc
 */
public class LingdocFileAiMeta
{
    private static final long serialVersionUID = 1L;

    /** 文件ID */
    private String fileId;

    /** 所属知识库ID */
    private String kbId;

    /** 文本解析状态：0待处理 1处理中 2已完成 3失败 */
    private String parseStatus;

    /** 解析错误信息 */
    private String parseError;

    /** 文本分块数量 */
    private Integer chunkCount;

    /** 向量化状态：0未处理 1处理中 2已完成 3失败 */
    private String embeddingStatus;

    /** AI一句话概括 */
    private String summary;

    /** AI提取关键词（JSON数组） */
    private String keywords;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    public String getFileId()
    {
        return fileId;
    }

    public void setFileId(String fileId)
    {
        this.fileId = fileId;
    }

    public String getKbId()
    {
        return kbId;
    }

    public void setKbId(String kbId)
    {
        this.kbId = kbId;
    }

    public String getParseStatus()
    {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus)
    {
        this.parseStatus = parseStatus;
    }

    public String getParseError()
    {
        return parseError;
    }

    public void setParseError(String parseError)
    {
        this.parseError = parseError;
    }

    public Integer getChunkCount()
    {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount)
    {
        this.chunkCount = chunkCount;
    }

    public String getEmbeddingStatus()
    {
        return embeddingStatus;
    }

    public void setEmbeddingStatus(String embeddingStatus)
    {
        this.embeddingStatus = embeddingStatus;
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    public String getKeywords()
    {
        return keywords;
    }

    public void setKeywords(String keywords)
    {
        this.keywords = keywords;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("fileId", getFileId())
            .append("kbId", getKbId())
            .append("parseStatus", getParseStatus())
            .append("parseError", getParseError())
            .append("chunkCount", getChunkCount())
            .append("embeddingStatus", getEmbeddingStatus())
            .append("summary", getSummary())
            .append("keywords", getKeywords())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
