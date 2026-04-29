package com.ruoyi.system.service.lingdoc.ai.impl;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.lingdoc.LingdocFormField;
import com.ruoyi.system.domain.lingdoc.ai.AiExtractResult;
import com.ruoyi.system.domain.lingdoc.ai.AiField;
import com.ruoyi.system.domain.lingdoc.ai.AiGenerateResult;
import com.ruoyi.system.domain.lingdoc.ai.AiReference;
import com.ruoyi.system.service.lingdoc.ai.IAiFormService;

/**
 * 本地 AI 服务实现（替代 Dify）
 * <p>
 * 直接调用本地 AI 服务 HTTP API，不再依赖 Dify 工作流。
 * 
 * @author lingdoc
 */
@Service
@Primary
public class LocalAiFormServiceImpl implements IAiFormService
{
    private static final Logger log = LoggerFactory.getLogger(LocalAiFormServiceImpl.class);

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** AI 服务 Base URL */
    @Value("${lingdoc.ai.local.base-url:http://localhost:8000}")
    private String aiBaseUrl;

    /** 内部共享 Token */
    @Value("${lingdoc.ai.local.internal-token:lingdoc-ai-2026-a7f3e9d2b8c1e4f5}")
    private String internalToken;

    private static final String API_FILL = "/api/ai/v1/form/fill";
    private static final String API_EXTRACT = "/api/ai/v1/form/extract";
    private static final String API_GENERATE = "/api/ai/v1/form/generate";

    @Override
    public AiExtractResult extract(String filePath, String originalFileName)
    {
        log.info("调用本地 AI 字段识别, fileName={}", originalFileName);

        try
        {
            // 构建请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Token", internalToken);

            Map<String, Object> body = new HashMap<>();
            body.put("task_id", "extract_" + System.currentTimeMillis());
            body.put("file_path", filePath);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            String url = aiBaseUrl + API_EXTRACT;
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful())
            {
                throw new RuntimeException("AI 服务调用失败: HTTP " + response.getStatusCode());
            }

            Map respBody = response.getBody();
            if (respBody == null || !Integer.valueOf(200).equals(respBody.get("code")))
            {
                throw new RuntimeException("AI 字段识别失败: " + respBody);
            }

            Map data = (Map) respBody.get("data");
            Map extracted = (Map) data.get("extracted");

            // 转换为 AiExtractResult
            AiExtractResult result = new AiExtractResult();
            List<AiField> fields = new ArrayList<>();
            List<AiReference> references = new ArrayList<>();

            if (extracted != null)
            {
                for (Object entryObj : extracted.entrySet())
                {
                    Map.Entry entry = (Map.Entry) entryObj;
                    String key = (String) entry.getKey();
                    Object value = entry.getValue();

                    AiField field = new AiField();
                    field.setFieldName(key);
                    field.setFieldLabel(key);
                    field.setFieldType("text");
                    field.setSuggestedValue(value != null ? value.toString() : "");
                    field.setConfidence(new BigDecimal("0.90"));
                    fields.add(field);
                }
            }

            result.setFields(fields);
            result.setReferences(references);
            result.setTokenCost(0);

            log.info("本地 AI 字段识别完成, fileName={}, fieldsCount={}",
                    originalFileName, fields.size());

            return result;

        }
        catch (Exception e)
        {
            log.error("本地 AI 字段识别异常", e);
            throw new RuntimeException("AI 字段识别失败: " + e.getMessage(), e);
        }
    }

    @Override
    public AiGenerateResult generate(String taskId, String originalFilePath, List<LingdocFormField> confirmedFields)
    {
        log.info("调用本地 AI 文档生成, taskId={}", taskId);

        try
        {
            // 1. 收集参考文档路径（从 confirmedFields 的 sourceDocName 提取，假设名称包含路径）
            List<String> referencePaths = new ArrayList<>();
            for (LingdocFormField field : confirmedFields)
            {
                if (StringUtils.isNotEmpty(field.getSourceDocName())
                        && !referencePaths.contains(field.getSourceDocName()))
                {
                    referencePaths.add(field.getSourceDocName());
                }
            }

            // 如果没有参考文档，使用当前模板作为参考
            if (referencePaths.isEmpty())
            {
                referencePaths.add(originalFilePath);
            }

            // 2. 构建端到端填表请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Token", internalToken);

            // 输出路径：在模板同目录下生成 _filled 后缀文件
            String outputPath = originalFilePath;
            int lastDot = originalFilePath.lastIndexOf('.');
            if (lastDot > 0)
            {
                outputPath = originalFilePath.substring(0, lastDot) + "_filled" + originalFilePath.substring(lastDot);
            }
            else
            {
                outputPath = originalFilePath + "_filled";
            }

            Map<String, Object> body = new HashMap<>();
            body.put("task_id", taskId);
            body.put("reference_paths", referencePaths);
            body.put("template_path", originalFilePath);
            body.put("output_path", outputPath);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            String url = aiBaseUrl + API_FILL;
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful())
            {
                throw new RuntimeException("AI 服务调用失败: HTTP " + response.getStatusCode());
            }

            Map respBody = response.getBody();
            if (respBody == null || !Integer.valueOf(200).equals(respBody.get("code")))
            {
                throw new RuntimeException("AI 文档生成失败: " + respBody);
            }

            Map data = (Map) respBody.get("data");

            // 3. 转换为 AiGenerateResult
            AiGenerateResult result = new AiGenerateResult();
            result.setFilledFilePath((String) data.get("output_path"));

            Map fillValues = (Map) data.get("fill_values");
            if (fillValues != null)
            {
                result.setFilledValues(fillValues);
            }
            else
            {
                result.setFilledValues(new HashMap<>());
            }

            result.setTokenCost(0);

            log.info("本地 AI 文档生成完成, taskId={}, hasFilePath={}",
                    taskId,
                    StringUtils.isNotEmpty(result.getFilledFilePath()));

            return result;

        }
        catch (Exception e)
            {
            log.error("本地 AI 文档生成异常", e);
            throw new RuntimeException("AI 文档生成失败: " + e.getMessage(), e);
        }
    }
}
