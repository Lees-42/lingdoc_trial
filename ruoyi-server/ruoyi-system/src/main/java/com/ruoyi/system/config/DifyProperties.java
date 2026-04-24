package com.ruoyi.system.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Dify AI 配置属性（支持多 Workflow）
 * <p>
 * 向后兼容：顶层 base-url / api-key / timeout 作为 default Workflow 的 fallback 配置。
 * 推荐用法：在 workflows Map 中为每个业务 Workflow 独立配置 base-url + api-key + timeout。
 *
 * @author lingdoc
 */
@Component
@ConfigurationProperties(prefix = "lingdoc.ai.dify")
public class DifyProperties
{
    /** 是否启用 Dify */
    private boolean enabled = true;

    /** 默认 Workflow 名称（当调用 runWorkflow 未指定名称时使用） */
    private String defaultWorkflow = "organize";

    /** Dify API 基础地址，如 http://localhost:5001/v1（向后兼容） */
    private String baseUrl;

    /** Dify API Key（向后兼容） */
    private String apiKey;

    /** 超时配置（向后兼容） */
    private Timeout timeout = new Timeout();

    /** 多 Workflow 配置 Map：key = workflow 名称，value = 独立配置 */
    private Map<String, WorkflowConfig> workflows = new HashMap<>();

    /**
     * 获取指定 Workflow 的配置。
     * <p>
     * 查找顺序：
     * 1. 从 workflows Map 中查找；
     * 2. 如果未找到且 name 等于 defaultWorkflow，fallback 到顶层配置（baseUrl / apiKey / timeout）。
     *
     * @param name Workflow 名称，如 "organize" / "form-extract" / "form-generate"
     * @return WorkflowConfig，不会返回 null
     * @throws IllegalArgumentException 配置未找到时
     */
    public WorkflowConfig getWorkflowConfig(String name)
    {
        if (workflows != null && workflows.containsKey(name))
        {
            return workflows.get(name);
        }

        // Fallback：如果是 default workflow，使用顶层配置
        if (defaultWorkflow != null && defaultWorkflow.equals(name)
                && (baseUrl != null || apiKey != null))
        {
            WorkflowConfig fallback = new WorkflowConfig();
            fallback.setBaseUrl(baseUrl);
            fallback.setApiKey(apiKey);
            fallback.setTimeout(timeout);
            return fallback;
        }

        throw new IllegalArgumentException(
                "Dify Workflow 配置未找到: " + name + ", 请在 lingdoc.ai.dify.workflows 中配置");
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getDefaultWorkflow()
    {
        return defaultWorkflow;
    }

    public void setDefaultWorkflow(String defaultWorkflow)
    {
        this.defaultWorkflow = defaultWorkflow;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    public Timeout getTimeout()
    {
        return timeout;
    }

    public void setTimeout(Timeout timeout)
    {
        this.timeout = timeout;
    }

    public Map<String, WorkflowConfig> getWorkflows()
    {
        return workflows;
    }

    public void setWorkflows(Map<String, WorkflowConfig> workflows)
    {
        this.workflows = workflows;
    }

    /**
     * 单个 Workflow 的配置
     */
    public static class WorkflowConfig
    {
        /** Dify API 基础地址 */
        private String baseUrl;

        /** Dify API Key */
        private String apiKey;

        /** 超时配置 */
        private Timeout timeout = new Timeout();

        public String getBaseUrl()
        {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl)
        {
            this.baseUrl = baseUrl;
        }

        public String getApiKey()
        {
            return apiKey;
        }

        public void setApiKey(String apiKey)
        {
            this.apiKey = apiKey;
        }

        public Timeout getTimeout()
        {
            return timeout;
        }

        public void setTimeout(Timeout timeout)
        {
            this.timeout = timeout;
        }
    }

    /**
     * 超时配置子类
     */
    public static class Timeout
    {
        /** 连接超时（毫秒），默认 10s */
        private int connect = 10000;

        /** 读取超时（毫秒），默认 30s */
        private int read = 30000;

        public int getConnect()
        {
            return connect;
        }

        public void setConnect(int connect)
        {
            this.connect = connect;
        }

        public int getRead()
        {
            return read;
        }

        public void setRead(int read)
        {
            this.read = read;
        }
    }
}
