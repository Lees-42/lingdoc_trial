package com.ruoyi.system.domain.lingdoc;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 标签定义表 lingdoc_tag
 * 
 * @author lingdoc
 */
public class LingdocTag
{
    private static final long serialVersionUID = 1L;

    /** 标签唯一ID */
    private String tagId;

    /** 标签名 */
    private String tagName;

    /** 标签颜色 */
    private String tagColor;

    /** 适用范围：A所有 F仅文件 D仅目录 */
    private String tagScope;

    /** 排序号 */
    private Integer sortOrder;

    /** 创建时间 */
    private Date createTime;

    public String getTagId()
    {
        return tagId;
    }

    public void setTagId(String tagId)
    {
        this.tagId = tagId;
    }

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

    public String getTagScope()
    {
        return tagScope;
    }

    public void setTagScope(String tagScope)
    {
        this.tagScope = tagScope;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
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
            .append("tagId", getTagId())
            .append("tagName", getTagName())
            .append("tagColor", getTagColor())
            .append("tagScope", getTagScope())
            .append("sortOrder", getSortOrder())
            .append("createTime", getCreateTime())
            .toString();
    }
}
