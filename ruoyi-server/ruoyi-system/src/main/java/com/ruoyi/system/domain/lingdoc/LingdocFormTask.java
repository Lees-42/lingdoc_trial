package com.ruoyi.system.domain.lingdoc;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 表格填写任务表 lingdoc_form_task
 * 
 * @author lingdoc
 */
public class LingdocFormTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private String taskId;

    /** 用户ID */
    private Long userId;

    /** 任务名称 */
    private String taskName;

    /** 原始空白文档文件ID */
    private String originalFileId;

    /** 原始文件存储路径 */
    private String originalFileUrl;

    /** 原始文件名 */
    private String originalFileName;

    /** AI填写后文档文件ID */
    private String filledFileId;

    /** 填写后文件存储路径 */
    private String filledFileUrl;

    /** 填写后文件名 */
    private String filledFileName;

    /** 状态：0待上传 1识别中 2待确认 3已生成 4失败 */
    private String status;

    /** AI原始识别结果JSON */
    private String aiResult;

    /** 字段总数 */
    private Integer fieldCount;

    /** 用户已确认字段数 */
    private Integer confirmedCount;

    /** Token消耗总量 */
    private Integer tokenCost;

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

    public String getOriginalFileId()
    {
        return originalFileId;
    }

    public void setOriginalFileId(String originalFileId)
    {
        this.originalFileId = originalFileId;
    }

    public String getOriginalFileUrl()
    {
        return originalFileUrl;
    }

    public void setOriginalFileUrl(String originalFileUrl)
    {
        this.originalFileUrl = originalFileUrl;
    }

    public String getOriginalFileName()
    {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName)
    {
        this.originalFileName = originalFileName;
    }

    public String getFilledFileId()
    {
        return filledFileId;
    }

    public void setFilledFileId(String filledFileId)
    {
        this.filledFileId = filledFileId;
    }

    public String getFilledFileUrl()
    {
        return filledFileUrl;
    }

    public void setFilledFileUrl(String filledFileUrl)
    {
        this.filledFileUrl = filledFileUrl;
    }

    public String getFilledFileName()
    {
        return filledFileName;
    }

    public void setFilledFileName(String filledFileName)
    {
        this.filledFileName = filledFileName;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getAiResult()
    {
        return aiResult;
    }

    public void setAiResult(String aiResult)
    {
        this.aiResult = aiResult;
    }

    public Integer getFieldCount()
    {
        return fieldCount;
    }

    public void setFieldCount(Integer fieldCount)
    {
        this.fieldCount = fieldCount;
    }

    public Integer getConfirmedCount()
    {
        return confirmedCount;
    }

    public void setConfirmedCount(Integer confirmedCount)
    {
        this.confirmedCount = confirmedCount;
    }

    public Integer getTokenCost()
    {
        return tokenCost;
    }

    public void setTokenCost(Integer tokenCost)
    {
        this.tokenCost = tokenCost;
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
            .append("originalFileId", getOriginalFileId())
            .append("originalFileUrl", getOriginalFileUrl())
            .append("originalFileName", getOriginalFileName())
            .append("filledFileId", getFilledFileId())
            .append("filledFileUrl", getFilledFileUrl())
            .append("filledFileName", getFilledFileName())
            .append("status", getStatus())
            .append("aiResult", getAiResult())
            .append("fieldCount", getFieldCount())
            .append("confirmedCount", getConfirmedCount())
            .append("tokenCost", getTokenCost())
            .append("errorMsg", getErrorMsg())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
