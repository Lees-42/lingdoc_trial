package com.ruoyi.system.domain.lingdoc;

import java.util.List;

/**
 * 表格字段批量更新请求 DTO
 * 
 * @author lingdoc
 */
public class FormFieldUpdateRequest
{
    /** 任务ID */
    private String taskId;

    /** 字段列表 */
    private List<LingdocFormField> fields;

    public String getTaskId()
    {
        return taskId;
    }

    public void setTaskId(String taskId)
    {
        this.taskId = taskId;
    }

    public List<LingdocFormField> getFields()
    {
        return fields;
    }

    public void setFields(List<LingdocFormField> fields)
    {
        this.fields = fields;
    }
}
