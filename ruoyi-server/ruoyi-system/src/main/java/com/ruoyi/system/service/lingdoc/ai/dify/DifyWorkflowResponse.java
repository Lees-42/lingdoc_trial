package com.ruoyi.system.service.lingdoc.ai.dify;

import java.util.Map;

/**
 * Dify Workflow 执行响应
 * <p>
 * outputs 字段使用通用 Map 类型，由各业务 Service 用 ObjectMapper 转换为具体模型。
 *
 * @author lingdoc
 */
public class DifyWorkflowResponse
{
    /** 工作流执行数据 */
    private WorkflowData data;

    public WorkflowData getData()
    {
        return data;
    }

    public void setData(WorkflowData data)
    {
        this.data = data;
    }

    /**
     * 工作流数据嵌套类
     */
    public static class WorkflowData
    {
        /** 执行记录 ID */
        private String id;

        /** 工作流 ID */
        private String workflowId;

        /** 执行状态：succeeded / failed */
        private String status;

        /** 业务输出（通用 Map，由调用方转换为目标模型） */
        private Map<String, Object> outputs;

        /** 执行耗时（秒） */
        private Double elapsedTime;

        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        public String getWorkflowId()
        {
            return workflowId;
        }

        public void setWorkflowId(String workflowId)
        {
            this.workflowId = workflowId;
        }

        public String getStatus()
        {
            return status;
        }

        public void setStatus(String status)
        {
            this.status = status;
        }

        public Map<String, Object> getOutputs()
        {
            return outputs;
        }

        public void setOutputs(Map<String, Object> outputs)
        {
            this.outputs = outputs;
        }

        public Double getElapsedTime()
        {
            return elapsedTime;
        }

        public void setElapsedTime(Double elapsedTime)
        {
            this.elapsedTime = elapsedTime;
        }
    }
}
