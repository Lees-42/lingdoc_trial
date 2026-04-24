package com.ruoyi.system.domain.lingdoc;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Vault文件索引表 lingdoc_file_index
 * 
 * @author lingdoc
 */
public class LingdocFileIndex extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 文件唯一ID */
    private String fileId;

    /** 所属用户ID */
    private Long userId;

    /** 文件名（含扩展名） */
    private String fileName;

    /** Vault内相对路径 */
    private String vaultPath;

    /** 绝对路径 */
    private String absPath;

    /** 文件类型 */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** SHA256校验值 */
    private String checksum;

    /** 子分类路径 */
    private String subPath;

    /** 来源：0手动上传 1自动规整 2表格助手生成 */
    private String sourceType;

    /** 文件文本内容 */
    private String fileContent;

    /** 大文件文本内容的外部存储路径 */
    private String contentPath;

    /** 文本内容字节数 */
    private Long contentSize;

    /** 是否有脱敏副本：0否 1是 */
    private String isDesensitized;

    public String getFileId()
    {
        return fileId;
    }

    public void setFileId(String fileId)
    {
        this.fileId = fileId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getVaultPath()
    {
        return vaultPath;
    }

    public void setVaultPath(String vaultPath)
    {
        this.vaultPath = vaultPath;
    }

    public String getAbsPath()
    {
        return absPath;
    }

    public void setAbsPath(String absPath)
    {
        this.absPath = absPath;
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

    public String getChecksum()
    {
        return checksum;
    }

    public void setChecksum(String checksum)
    {
        this.checksum = checksum;
    }

    public String getSubPath()
    {
        return subPath;
    }

    public void setSubPath(String subPath)
    {
        this.subPath = subPath;
    }

    public String getSourceType()
    {
        return sourceType;
    }

    public void setSourceType(String sourceType)
    {
        this.sourceType = sourceType;
    }

    public String getFileContent()
    {
        return fileContent;
    }

    public void setFileContent(String fileContent)
    {
        this.fileContent = fileContent;
    }

    public String getContentPath()
    {
        return contentPath;
    }

    public void setContentPath(String contentPath)
    {
        this.contentPath = contentPath;
    }

    public Long getContentSize()
    {
        return contentSize;
    }

    public void setContentSize(Long contentSize)
    {
        this.contentSize = contentSize;
    }

    public String getIsDesensitized()
    {
        return isDesensitized;
    }

    public void setIsDesensitized(String isDesensitized)
    {
        this.isDesensitized = isDesensitized;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("fileId", getFileId())
            .append("userId", getUserId())
            .append("fileName", getFileName())
            .append("vaultPath", getVaultPath())
            .append("absPath", getAbsPath())
            .append("fileType", getFileType())
            .append("fileSize", getFileSize())
            .append("checksum", getChecksum())
            .append("subPath", getSubPath())
            .append("sourceType", getSourceType())
            .append("fileContent", getFileContent())
            .append("contentPath", getContentPath())
            .append("contentSize", getContentSize())
            .append("isDesensitized", getIsDesensitized())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
