package com.ruoyi.system.domain.lingdoc;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 任务参考文档关联表 lingdoc_form_reference
 * 
 * @author lingdoc
 */
public class LingdocFormReference extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 关联ID */
    private String refId;

    /** 任务ID */
    private String taskId;

    /** Vault文档ID */
    private String docId;

    /** 文档名称 */
    private String docName;

    /** 文档存储路径 */
    private String docPath;

    /** 文档类型 */
    private String docType;

    /** 相关性评分（0.00~1.00） */
    private BigDecimal relevance;

    /** 是否被选中：0否 1是 */
    private String isSelected;

    public String getRefId()
    {
        return refId;
    }

    public void setRefId(String refId)
    {
        this.refId = refId;
    }

    public String getTaskId()
    {
        return taskId;
    }

    public void setTaskId(String taskId)
    {
        this.taskId = taskId;
    }

    public String getDocId()
    {
        return docId;
    }

    public void setDocId(String docId)
    {
        this.docId = docId;
    }

    public String getDocName()
    {
        return docName;
    }

    public void setDocName(String docName)
    {
        this.docName = docName;
    }

    public String getDocPath()
    {
        return docPath;
    }

    public void setDocPath(String docPath)
    {
        this.docPath = docPath;
    }

    public String getDocType()
    {
        return docType;
    }

    public void setDocType(String docType)
    {
        this.docType = docType;
    }

    public BigDecimal getRelevance()
    {
        return relevance;
    }

    public void setRelevance(BigDecimal relevance)
    {
        this.relevance = relevance;
    }

    public String getIsSelected()
    {
        return isSelected;
    }

    public void setIsSelected(String isSelected)
    {
        this.isSelected = isSelected;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("refId", getRefId())
            .append("taskId", getTaskId())
            .append("docId", getDocId())
            .append("docName", getDocName())
            .append("docPath", getDocPath())
            .append("docType", getDocType())
            .append("relevance", getRelevance())
            .append("isSelected", getIsSelected())
            .append("createTime", getCreateTime())
            .toString();
    }
}
