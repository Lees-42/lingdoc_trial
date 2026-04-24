package com.ruoyi.system.service.lingdoc.ai.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.lingdoc.LingdocFormField;
import com.ruoyi.system.domain.lingdoc.ai.AiExtractResult;
import com.ruoyi.system.domain.lingdoc.ai.AiField;
import com.ruoyi.system.domain.lingdoc.ai.AiGenerateResult;
import com.ruoyi.system.domain.lingdoc.ai.AiReference;
import com.ruoyi.system.service.lingdoc.ai.IAiFormService;
import com.ruoyi.system.service.lingdoc.ai.dify.DifyFormExtractOutput;
import com.ruoyi.system.service.lingdoc.ai.dify.DifyFormGenerateOutput;
import com.ruoyi.system.service.lingdoc.ai.dify.DifyWorkflowClient;

/**
 * 表格填写助手 Dify 实现
 * <p>
 * 调用两个 Workflow：
 * 1. form-extract：字段识别
 * 2. form-generate：文档生成
 *
 * @author lingdoc
 */
@Service
@Primary
public class DifyAiFormServiceImpl implements IAiFormService
{
    private static final Logger log = LoggerFactory.getLogger(DifyAiFormServiceImpl.class);

    @Autowired
    private DifyWorkflowClient difyClient;

    @Autowired
    ObjectMapper objectMapper;

    /** form-extract Workflow 配置名称 */
    private static final String WORKFLOW_EXTRACT = "form-extract";

    /** form-generate Workflow 配置名称 */
    private static final String WORKFLOW_GENERATE = "form-generate";

    @Override
    public AiExtractResult extract(String filePath, String originalFileName)
    {
        String fileContent = readFileContent(filePath, originalFileName);
        String fileType = getExtension(originalFileName);

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("fileName", originalFileName);
        inputs.put("fileContent", fileContent != null ? fileContent : "");
        inputs.put("fileType", fileType);

        log.info("调用 Dify 表格字段识别, fileName={}", originalFileName);

        String userId = String.valueOf(SecurityUtils.getUserId());
        var resp = difyClient.runWorkflow(WORKFLOW_EXTRACT, inputs, userId);

        if (resp == null || resp.getData() == null)
        {
            throw new RuntimeException("Dify 表格识别返回空响应");
        }
        if (!"succeeded".equals(resp.getData().getStatus()))
        {
            throw new RuntimeException("Dify 表格识别工作流执行失败, status=" + resp.getData().getStatus());
        }

        DifyFormExtractOutput output = objectMapper.convertValue(
                resp.getData().getOutputs(), DifyFormExtractOutput.class);

        AiExtractResult result = convertToExtractResult(output);

        log.info("Dify 表格字段识别完成, fileName={}, fieldsCount={}, refsCount={}",
                originalFileName,
                result.getFields() != null ? result.getFields().size() : 0,
                result.getReferences() != null ? result.getReferences().size() : 0);

        return result;
    }

    @Override
    public AiGenerateResult generate(String taskId, String originalFilePath, List<LingdocFormField> confirmedFields)
    {
        String fileType = getExtension(originalFilePath);
        String confirmedFieldsJson = buildConfirmedFieldsJson(confirmedFields);

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("originalFilePath", originalFilePath);
        inputs.put("fileType", fileType);
        inputs.put("confirmedFields", confirmedFieldsJson);

        log.info("调用 Dify 文档生成, taskId={}", taskId);

        String userId = String.valueOf(SecurityUtils.getUserId());
        var resp = difyClient.runWorkflow(WORKFLOW_GENERATE, inputs, userId);

        if (resp == null || resp.getData() == null)
        {
            throw new RuntimeException("Dify 文档生成返回空响应");
        }
        if (!"succeeded".equals(resp.getData().getStatus()))
        {
            throw new RuntimeException("Dify 文档生成工作流执行失败, status=" + resp.getData().getStatus());
        }

        DifyFormGenerateOutput output = objectMapper.convertValue(
                resp.getData().getOutputs(), DifyFormGenerateOutput.class);

        AiGenerateResult result = convertToGenerateResult(output);

        log.info("Dify 文档生成完成, taskId={}, hasFilePath={}",
                taskId,
                StringUtils.isNotEmpty(result.getFilledFilePath()));

        return result;
    }

    // ---------- 私有转换方法 ----------

    private AiExtractResult convertToExtractResult(DifyFormExtractOutput output)
    {
        AiExtractResult result = new AiExtractResult();

        if (output == null)
        {
            result.setFields(new ArrayList<>());
            result.setReferences(new ArrayList<>());
            result.setTokenCost(0);
            return result;
        }

        // 转换字段列表
        List<AiField> fields = new ArrayList<>();
        if (output.getFields() != null)
        {
            for (DifyFormExtractOutput.DifyFieldOutput f : output.getFields())
            {
                AiField field = new AiField();
                field.setFieldName(f.getFieldName());
                field.setFieldType(f.getFieldType());
                field.setFieldLabel(f.getFieldLabel());
                field.setSuggestedValue(f.getSuggestedValue());
                field.setConfidence(f.getConfidence());
                field.setSourceDocId(f.getSourceDocId());
                field.setSourceDocName(f.getSourceDocName());
                field.setSortOrder(f.getSortOrder());
                fields.add(field);
            }
        }
        result.setFields(fields);

        // 转换参考文档列表
        List<AiReference> references = new ArrayList<>();
        if (output.getReferences() != null)
        {
            for (DifyFormExtractOutput.DifyReferenceOutput r : output.getReferences())
            {
                AiReference ref = new AiReference();
                ref.setDocId(r.getDocId());
                ref.setDocName(r.getDocName());
                ref.setDocPath(r.getDocPath());
                ref.setDocType(r.getDocType());
                ref.setRelevance(r.getRelevance());
                references.add(ref);
            }
        }
        result.setReferences(references);

        result.setTokenCost(output.getTokenCost() != null ? output.getTokenCost() : 0);
        return result;
    }

    private AiGenerateResult convertToGenerateResult(DifyFormGenerateOutput output)
    {
        AiGenerateResult result = new AiGenerateResult();

        if (output == null)
        {
            result.setFilledValues(new HashMap<>());
            result.setTokenCost(0);
            return result;
        }

        result.setFilledFilePath(output.getFilledFilePath());
        result.setFilledValues(output.getFilledValues());
        result.setTokenCost(output.getTokenCost() != null ? output.getTokenCost() : 0);
        return result;
    }

    // ---------- 私有工具方法 ----------

    /**
     * 读取文件文本内容（尽量提取）
     */
    private String readFileContent(String filePath, String fileName)
    {
        String ext = getExtension(fileName);

        // HTML 文件：用 Jsoup 提取纯文本
        if ("html".equalsIgnoreCase(ext) || "htm".equalsIgnoreCase(ext))
        {
            try
            {
                org.jsoup.nodes.Document doc = Jsoup.parse(Paths.get(filePath).toFile(), "UTF-8");
                return doc.text();
            }
            catch (IOException e)
            {
                log.warn("读取 HTML 文本失败: {}", filePath);
                return "";
            }
        }

        // 纯文本文件
        List<String> textTypes = List.of("txt", "md", "csv", "json", "xml", "yaml", "yml", "js", "css", "java", "py");
        if (textTypes.contains(ext.toLowerCase()))
        {
            try
            {
                return Files.readString(Paths.get(filePath));
            }
            catch (IOException e)
            {
                log.warn("读取文本文件失败: {}", filePath);
                return "";
            }
        }

        // 二进制文件（docx/pdf/xlsx 等）：目前无法提取文本，传空字符串
        // TODO: 后续可接入 Apache Tika / python-docx / pdfbox 等工具提取文本
        return "";
    }

    /**
     * 将已确认字段列表转为 JSON 字符串（供 Dify inputs 使用）
     */
    private String buildConfirmedFieldsJson(List<LingdocFormField> confirmedFields)
    {
        List<Map<String, String>> list = new ArrayList<>();
        if (confirmedFields != null)
        {
            for (LingdocFormField field : confirmedFields)
            {
                Map<String, String> item = new HashMap<>();
                item.put("fieldName", field.getFieldName());
                item.put("fieldType", field.getFieldType());

                // fieldValue 优先使用用户确认值，否则使用 AI 建议值
                String value = field.getUserValue();
                if (StringUtils.isEmpty(value))
                {
                    value = field.getAiValue();
                }
                item.put("fieldValue", value != null ? value : "");
                list.add(item);
            }
        }

        try
        {
            return objectMapper.writeValueAsString(list);
        }
        catch (JsonProcessingException e)
        {
            log.error("序列化 confirmedFields 失败", e);
            return "[]";
        }
    }

    private String getExtension(String fileName)
    {
        if (StringUtils.isEmpty(fileName) || fileName.lastIndexOf('.') < 0)
        {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
