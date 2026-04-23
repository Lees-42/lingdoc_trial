package com.ruoyi.system.domain.lingdoc;

import java.util.List;

/**
 * 上传文件确认归档请求 DTO
 * 
 * @author lingdoc
 */
public class LingdocUploadConfirmRequest
{
    /** 文件ID（inbox_id） */
    private String fileId;

    /** 用户确认后的文件名 */
    private String suggestedName;

    /** 用户确认后的路径（如 '工作/合同' 或 '/'） */
    private String suggestedPath;

    /** 标签ID列表 */
    private List<String> tagIds;

    /** 备注 */
    private String remark;

    public String getFileId()
    {
        return fileId;
    }

    public void setFileId(String fileId)
    {
        this.fileId = fileId;
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

    public List<String> getTagIds()
    {
        return tagIds;
    }

    public void setTagIds(List<String> tagIds)
    {
        this.tagIds = tagIds;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}
