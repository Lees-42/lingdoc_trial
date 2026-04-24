package com.ruoyi.system.domain.lingdoc;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 脱敏文件表 lingdoc_desensitized_file
 * 
 * @author lingdoc
 */
public class LingdocDesensitizedFile
{
    private static final long serialVersionUID = 1L;

    /** 脱敏记录ID */
    private String desId;

    /** 原文件ID */
    private String fileId;

    /** 脱敏文件物理路径 */
    private String desPath;

    /** 脱敏后的纯文本内容 */
    private String desContent;

    /** 脱敏内容校验（SHA-256） */
    private String desChecksum;

    /** 使用的脱敏规则ID */
    private String ruleId;

    /** 创建时间 */
    private Date createTime;

    public String getDesId()
    {
        return desId;
    }

    public void setDesId(String desId)
    {
        this.desId = desId;
    }

    public String getFileId()
    {
        return fileId;
    }

    public void setFileId(String fileId)
    {
        this.fileId = fileId;
    }

    public String getDesPath()
    {
        return desPath;
    }

    public void setDesPath(String desPath)
    {
        this.desPath = desPath;
    }

    public String getDesContent()
    {
        return desContent;
    }

    public void setDesContent(String desContent)
    {
        this.desContent = desContent;
    }

    public String getDesChecksum()
    {
        return desChecksum;
    }

    public void setDesChecksum(String desChecksum)
    {
        this.desChecksum = desChecksum;
    }

    public String getRuleId()
    {
        return ruleId;
    }

    public void setRuleId(String ruleId)
    {
        this.ruleId = ruleId;
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
            .append("desId", getDesId())
            .append("fileId", getFileId())
            .append("desPath", getDesPath())
            .append("desContent", getDesContent())
            .append("desChecksum", getDesChecksum())
            .append("ruleId", getRuleId())
            .append("createTime", getCreateTime())
            .toString();
    }
}
