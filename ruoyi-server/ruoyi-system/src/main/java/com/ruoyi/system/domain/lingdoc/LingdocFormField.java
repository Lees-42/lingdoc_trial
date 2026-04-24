package com.ruoyi.system.domain.lingdoc;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 表格字段表 lingdoc_form_field
 * 
 * @author lingdoc
 */
public class LingdocFormField extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 字段ID */
    private String fieldId;

    /** 所属任务ID */
    private String taskId;

    /** 字段名称 */
    private String fieldName;

    /** 字段类型：text/date/number/select/checkbox */
    private String fieldType;

    /** 字段在文档中的原始标签文本 */
    private String fieldLabel;

    /** AI建议的填写值 */
    private String aiValue;

    /** 用户最终确认的值 */
    private String userValue;

    /** 是否已确认：0否 1是 */
    private String isConfirmed;

    /** AI置信度（0.00~1.00） */
    private BigDecimal confidence;

    /** 值来源的Vault文档ID */
    private String sourceDocId;

    /** 来源文档名称 */
    private String sourceDocName;

    /** 字段排序号 */
    private Integer sortOrder;

    public String getFieldId()
    {
        return fieldId;
    }

    public void setFieldId(String fieldId)
    {
        this.fieldId = fieldId;
    }

    public String getTaskId()
    {
        return taskId;
    }

    public void setTaskId(String taskId)
    {
        this.taskId = taskId;
    }

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

    public String getAiValue()
    {
        return aiValue;
    }

    public void setAiValue(String aiValue)
    {
        this.aiValue = aiValue;
    }

    public String getUserValue()
    {
        return userValue;
    }

    public void setUserValue(String userValue)
    {
        this.userValue = userValue;
    }

    public String getIsConfirmed()
    {
        return isConfirmed;
    }

    public void setIsConfirmed(String isConfirmed)
    {
        this.isConfirmed = isConfirmed;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("fieldId", getFieldId())
            .append("taskId", getTaskId())
            .append("fieldName", getFieldName())
            .append("fieldType", getFieldType())
            .append("fieldLabel", getFieldLabel())
            .append("aiValue", getAiValue())
            .append("userValue", getUserValue())
            .append("isConfirmed", getIsConfirmed())
            .append("confidence", getConfidence())
            .append("sourceDocId", getSourceDocId())
            .append("sourceDocName", getSourceDocName())
            .append("sortOrder", getSortOrder())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
