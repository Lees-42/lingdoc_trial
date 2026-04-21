package com.ruoyi.system.domain.lingdoc;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 文件版本记录表 lingdoc_file_version
 * 
 * @author lingdoc
 */
public class LingdocFileVersion
{
    private static final long serialVersionUID = 1L;

    /** 版本ID */
    private String versionId;

    /** 原文件ID */
    private String fileId;

    /** 版本号 */
    private Integer versionNo;

    /** 版本快照存储路径 */
    private String snapshotPath;

    /** 快照文件大小 */
    private Long snapshotSize;

    /** 操作类型：0编辑 1重命名 2移动 3回滚 */
    private String operationType;

    /** 版本文件checksum */
    private String checksum;

    /** 操作人 */
    private Long operatorId;

    /** 创建时间 */
    private Date createTime;

    public String getVersionId()
    {
        return versionId;
    }

    public void setVersionId(String versionId)
    {
        this.versionId = versionId;
    }

    public String getFileId()
    {
        return fileId;
    }

    public void setFileId(String fileId)
    {
        this.fileId = fileId;
    }

    public Integer getVersionNo()
    {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo)
    {
        this.versionNo = versionNo;
    }

    public String getSnapshotPath()
    {
        return snapshotPath;
    }

    public void setSnapshotPath(String snapshotPath)
    {
        this.snapshotPath = snapshotPath;
    }

    public Long getSnapshotSize()
    {
        return snapshotSize;
    }

    public void setSnapshotSize(Long snapshotSize)
    {
        this.snapshotSize = snapshotSize;
    }

    public String getOperationType()
    {
        return operationType;
    }

    public void setOperationType(String operationType)
    {
        this.operationType = operationType;
    }

    public String getChecksum()
    {
        return checksum;
    }

    public void setChecksum(String checksum)
    {
        this.checksum = checksum;
    }

    public Long getOperatorId()
    {
        return operatorId;
    }

    public void setOperatorId(Long operatorId)
    {
        this.operatorId = operatorId;
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
            .append("versionId", getVersionId())
            .append("fileId", getFileId())
            .append("versionNo", getVersionNo())
            .append("snapshotPath", getSnapshotPath())
            .append("snapshotSize", getSnapshotSize())
            .append("operationType", getOperationType())
            .append("checksum", getChecksum())
            .append("operatorId", getOperatorId())
            .append("createTime", getCreateTime())
            .toString();
    }
}
