package com.ruoyi.system.domain.lingdoc;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * PaddleOCR 识别任务表 lingdoc_ocr_task
 * 
 * @author lingdoc
 */
public class PaddleOcrTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private String taskId;

    /** 用户ID */
    private Long userId;

    /** 任务名称 */
    private String taskName;

    /** 原始文件存储路径 */
    private String fileUrl;

    /** 原始文件名 */
    private String fileName;

    /** 文件类型：pdf/docx/doc/jpg/png */
    private String fileType;

    /** 状态：0待处理 1处理中 2成功 3失败 */
    private String status;

    /** OCR识别结果JSON */
    private String resultJson;

    /** 总页数 */
    private Integer pageCount;

    /** 处理耗时（毫秒） */
    private Long processTime;

    /** 平均单页耗时（毫秒） */
    private Long avgPageTime;

    /** 识别字符总数 */
    private Integer charCount;

    /** 错误信息 */
    private String errorMsg;

    public String getTaskId()
    {
        return taskId;
    }

    public void setTaskId(String taskId)
    {
        this.taskId = taskId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getTaskName()
    {
        return taskName;
    }

    public void setTaskName(String taskName)
    {
        this.taskName = taskName;
    }

    public String getFileUrl()
    {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl)
    {
        this.fileUrl = fileUrl;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getFileType()
    {
        return fileType;
    }

    public void setFileType(String fileType)
    {
        this.fileType = fileType;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getResultJson()
    {
        return resultJson;
    }

    public void setResultJson(String resultJson)
    {
        this.resultJson = resultJson;
    }

    public Integer getPageCount()
    {
        return pageCount;
    }

    public void setPageCount(Integer pageCount)
    {
        this.pageCount = pageCount;
    }

    public Long getProcessTime()
    {
        return processTime;
    }

    public void setProcessTime(Long processTime)
    {
        this.processTime = processTime;
    }

    public Long getAvgPageTime()
    {
        return avgPageTime;
    }

    public void setAvgPageTime(Long avgPageTime)
    {
        this.avgPageTime = avgPageTime;
    }

    public Integer getCharCount()
    {
        return charCount;
    }

    public void setCharCount(Integer charCount)
    {
        this.charCount = charCount;
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
            .append("taskId", getTaskId())
            .append("userId", getUserId())
            .append("taskName", getTaskName())
            .append("fileUrl", getFileUrl())
            .append("fileName", getFileName())
            .append("fileType", getFileType())
            .append("status", getStatus())
            .append("resultJson", getResultJson())
            .append("pageCount", getPageCount())
            .append("processTime", getProcessTime())
            .append("avgPageTime", getAvgPageTime())
            .append("charCount", getCharCount())
            .append("errorMsg", getErrorMsg())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
