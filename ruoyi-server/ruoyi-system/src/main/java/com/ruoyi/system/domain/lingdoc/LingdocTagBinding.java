package com.ruoyi.system.domain.lingdoc;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 标签绑定表 lingdoc_tag_binding
 * 
 * @author lingdoc
 */
public class LingdocTagBinding
{
    private static final long serialVersionUID = 1L;

    /** 绑定记录ID */
    private String bindingId;

    /** 目标类型：F文件 D目录 */
    private String targetType;

    /** 目标标识：文件时为file_id，目录时为目录路径 */
    private String targetId;

    /** 标签ID */
    private String tagId;

    /** 绑定类型：0直接绑定 1继承自父目录 */
    private String bindType;

    /** 创建时间 */
    private Date createTime;

    public String getBindingId()
    {
        return bindingId;
    }

    public void setBindingId(String bindingId)
    {
        this.bindingId = bindingId;
    }

    public String getTargetType()
    {
        return targetType;
    }

    public void setTargetType(String targetType)
    {
        this.targetType = targetType;
    }

    public String getTargetId()
    {
        return targetId;
    }

    public void setTargetId(String targetId)
    {
        this.targetId = targetId;
    }

    public String getTagId()
    {
        return tagId;
    }

    public void setTagId(String tagId)
    {
        this.tagId = tagId;
    }

    public String getBindType()
    {
        return bindType;
    }

    public void setBindType(String bindType)
    {
        this.bindType = bindType;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("bindingId", getBindingId())
            .append("targetType", getTargetType())
            .append("targetId", getTargetId())
            .append("tagId", getTagId())
            .append("bindType", getBindType())
            .append("createTime", getCreateTime())
            .toString();
    }
}
