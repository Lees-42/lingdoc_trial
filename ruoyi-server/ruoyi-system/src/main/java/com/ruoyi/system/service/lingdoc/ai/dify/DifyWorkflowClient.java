package com.ruoyi.system.service.lingdoc.ai.dify;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import com.ruoyi.system.config.DifyProperties;
import com.ruoyi.system.config.DifyProperties.WorkflowConfig;

/**
 * Dify Workflow HTTP 客户端（支持多 Workflow 配置）
 * <p>
 * 每个 Workflow 可独立配置 base-url + api-key + timeout。
 * RestTemplate 按 Workflow 名称缓存，避免重复创建。
 * <p>
 * 注意：当 lingdoc.ai.dify.enabled=false 时，此 Bean 不会创建。
 *
 * @author lingdoc
 */
@Component
@Conditional(DifyWorkflowClient.DifyEnabledCondition.class)
public class DifyWorkflowClient
{
    /**
     * 条件：仅当 lingdoc.ai.dify.enabled=true 时创建此 Bean
     */
    public static class DifyEnabledCondition implements Condition
    {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
        {
            String enabled = context.getEnvironment().getProperty("lingdoc.ai.dify.enabled", "true");
            return "true".equalsIgnoreCase(enabled);
        }
    }
    @Autowired
    private DifyProperties difyProps;

    /** 按 Workflow 名称缓存 RestTemplate */
    private final Map<String, RestTemplate> restTemplateCache = new HashMap<>();

    /**
     * 调用默认 Workflow（向后兼容）
     *
     * @param inputs Workflow 输入参数
     * @param userId 用户标识
     * @return Dify 响应
     */
    public DifyWorkflowResponse runWorkflow(Map<String, Object> inputs, String userId)
    {
        return runWorkflow(difyProps.getDefaultWorkflow(), inputs, userId);
    }

    /**
     * 调用指定名称的 Workflow
     *
     * @param workflowName Workflow 配置名称，如 "organize" / "form-extract" / "form-generate"
     * @param inputs       Workflow 输入参数（已排除 response_mode 和 user）
     * @param userId       用户标识
     * @return Dify 响应
     * @throws IllegalArgumentException Workflow 配置未找到
     */
    public DifyWorkflowResponse runWorkflow(String workflowName,
                                            Map<String, Object> inputs,
                                            String userId)
    {
        WorkflowConfig config = difyProps.getWorkflowConfig(workflowName);
        RestTemplate restTemplate = getRestTemplate(workflowName, config);

        String url = config.getBaseUrl() + "/workflows/run";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + config.getApiKey());

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs);
        body.put("response_mode", "blocking");
        body.put("user", "user_" + userId);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForObject(url, entity, DifyWorkflowResponse.class);
    }

    /**
     * 获取或创建指定 Workflow 的 RestTemplate（带缓存）
     */
    private synchronized RestTemplate getRestTemplate(String workflowName, WorkflowConfig config)
    {
        RestTemplate cached = restTemplateCache.get(workflowName);
        if (cached != null)
        {
            return cached;
        }

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getTimeout().getConnect());
        factory.setReadTimeout(config.getTimeout().getRead());

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplateCache.put(workflowName, restTemplate);
        return restTemplate;
    }
}
