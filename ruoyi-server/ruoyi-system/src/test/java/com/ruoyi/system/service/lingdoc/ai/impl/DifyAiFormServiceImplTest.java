package com.ruoyi.system.service.lingdoc.ai.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.lingdoc.LingdocFormField;
import com.ruoyi.system.domain.lingdoc.ai.AiExtractResult;
import com.ruoyi.system.domain.lingdoc.ai.AiGenerateResult;
import com.ruoyi.system.service.lingdoc.ai.dify.DifyFormExtractOutput;
import com.ruoyi.system.service.lingdoc.ai.dify.DifyFormGenerateOutput;
import com.ruoyi.system.service.lingdoc.ai.dify.DifyWorkflowClient;
import com.ruoyi.system.service.lingdoc.ai.dify.DifyWorkflowResponse;

/**
 * Dify 表格填写服务单元测试
 */
class DifyAiFormServiceImplTest
{
    @Mock
    private DifyWorkflowClient difyClient;

    @InjectMocks
    private DifyAiFormServiceImpl formService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        formService.objectMapper = objectMapper;
    }

    @Test
    void testExtractSuccess()
    {
        DifyWorkflowResponse response = buildExtractSuccessResponse();
        when(difyClient.runWorkflow(eq("form-extract"), anyMap(), anyString())).thenReturn(response);

        try (MockedStatic<SecurityUtils> security = Mockito.mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::getUserId).thenReturn(1L);

            AiExtractResult result = formService.extract("/tmp/test.docx", "奖学金申请表.docx");

            assertNotNull(result);
            assertNotNull(result.getFields());
            assertEquals(2, result.getFields().size());

            assertEquals("姓名", result.getFields().get(0).getFieldName());
            assertEquals("text", result.getFields().get(0).getFieldType());
            assertEquals("张三", result.getFields().get(0).getSuggestedValue());

            assertEquals("学号", result.getFields().get(1).getFieldName());
            assertEquals("2023001001", result.getFields().get(1).getSuggestedValue());

            assertNotNull(result.getReferences());
            assertEquals(1, result.getReferences().size());
            assertEquals("成绩单.pdf", result.getReferences().get(0).getDocName());

            assertEquals(850, result.getTokenCost());
        }
    }

    @Test
    void testExtractDifyFailed()
    {
        DifyWorkflowResponse response = new DifyWorkflowResponse();
        DifyWorkflowResponse.WorkflowData data = new DifyWorkflowResponse.WorkflowData();
        data.setStatus("failed");
        response.setData(data);

        when(difyClient.runWorkflow(eq("form-extract"), anyMap(), anyString())).thenReturn(response);

        try (MockedStatic<SecurityUtils> security = Mockito.mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::getUserId).thenReturn(1L);

            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                formService.extract("/tmp/test.docx", "奖学金申请表.docx");
            });

            assertTrue(exception.getMessage().contains("Dify 表格识别工作流执行失败"));
        }
    }

    @Test
    void testGenerateSuccess()
    {
        DifyWorkflowResponse response = buildGenerateSuccessResponse();
        when(difyClient.runWorkflow(eq("form-generate"), anyMap(), anyString())).thenReturn(response);

        try (MockedStatic<SecurityUtils> security = Mockito.mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::getUserId).thenReturn(1L);

            List<LingdocFormField> confirmedFields = Arrays.asList(
                buildField("姓名", "text", "张三"),
                buildField("学号", "text", "2023001001")
            );

            AiGenerateResult result = formService.generate("task_001", "/tmp/test.docx", confirmedFields);

            assertNotNull(result);
            assertNotNull(result.getFilledFilePath());
            assertEquals("/tmp/filled.docx", result.getFilledFilePath());
            assertNotNull(result.getFilledValues());
            assertEquals("张三", result.getFilledValues().get("姓名"));
            assertEquals(1200, result.getTokenCost());
        }
    }

    @Test
    void testGenerateWithFilledValuesOnly()
    {
        // 方式 B：AI 只返回 filledValues，不生成文件
        DifyWorkflowResponse response = new DifyWorkflowResponse();
        DifyWorkflowResponse.WorkflowData data = new DifyWorkflowResponse.WorkflowData();
        data.setStatus("succeeded");

        DifyFormGenerateOutput output = new DifyFormGenerateOutput();
        output.setFilledFilePath(null);
        output.setFilledValues(Map.of("姓名", "张三", "学号", "2023001001"));
        output.setTokenCost(800);

        data.setOutputs(objectMapper.convertValue(output, Map.class));
        response.setData(data);

        when(difyClient.runWorkflow(eq("form-generate"), anyMap(), anyString())).thenReturn(response);

        try (MockedStatic<SecurityUtils> security = Mockito.mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::getUserId).thenReturn(1L);

            List<LingdocFormField> confirmedFields = Arrays.asList(
                buildField("姓名", "text", "张三"),
                buildField("学号", "text", "2023001001")
            );

            AiGenerateResult result = formService.generate("task_001", "/tmp/test.docx", confirmedFields);

            assertNull(result.getFilledFilePath());
            assertNotNull(result.getFilledValues());
            assertEquals("张三", result.getFilledValues().get("姓名"));
            assertEquals(800, result.getTokenCost());
        }
    }

    @Test
    void testExtractEmptyResponse()
    {
        DifyWorkflowResponse response = new DifyWorkflowResponse();
        DifyWorkflowResponse.WorkflowData data = new DifyWorkflowResponse.WorkflowData();
        data.setStatus("succeeded");
        data.setOutputs(new HashMap<>());
        response.setData(data);

        when(difyClient.runWorkflow(eq("form-extract"), anyMap(), anyString())).thenReturn(response);

        try (MockedStatic<SecurityUtils> security = Mockito.mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::getUserId).thenReturn(1L);

            AiExtractResult result = formService.extract("/tmp/test.docx", "奖学金申请表.docx");

            assertNotNull(result);
            assertTrue(result.getFields().isEmpty());
            assertTrue(result.getReferences().isEmpty());
            assertEquals(0, result.getTokenCost());
        }
    }

    // ---------- 辅助方法 ----------

    private DifyWorkflowResponse buildExtractSuccessResponse()
    {
        DifyWorkflowResponse response = new DifyWorkflowResponse();
        DifyWorkflowResponse.WorkflowData data = new DifyWorkflowResponse.WorkflowData();
        data.setStatus("succeeded");

        DifyFormExtractOutput output = new DifyFormExtractOutput();

        DifyFormExtractOutput.DifyFieldOutput field1 = new DifyFormExtractOutput.DifyFieldOutput();
        field1.setFieldName("姓名");
        field1.setFieldType("text");
        field1.setFieldLabel("姓名");
        field1.setSuggestedValue("张三");
        field1.setConfidence(new BigDecimal("0.92"));
        field1.setSourceDocId("doc_001");
        field1.setSourceDocName("成绩单.pdf");
        field1.setSortOrder(1);

        DifyFormExtractOutput.DifyFieldOutput field2 = new DifyFormExtractOutput.DifyFieldOutput();
        field2.setFieldName("学号");
        field2.setFieldType("text");
        field2.setFieldLabel("学号");
        field2.setSuggestedValue("2023001001");
        field2.setConfidence(new BigDecimal("0.95"));
        field2.setSourceDocId("doc_001");
        field2.setSourceDocName("成绩单.pdf");
        field2.setSortOrder(2);

        output.setFields(Arrays.asList(field1, field2));

        DifyFormExtractOutput.DifyReferenceOutput ref1 = new DifyFormExtractOutput.DifyReferenceOutput();
        ref1.setDocId("doc_001");
        ref1.setDocName("成绩单.pdf");
        ref1.setDocPath("学习/成绩单.pdf");
        ref1.setDocType("pdf");
        ref1.setRelevance(new BigDecimal("0.95"));

        output.setReferences(Arrays.asList(ref1));
        output.setTokenCost(850);

        data.setOutputs(objectMapper.convertValue(output, Map.class));
        response.setData(data);
        return response;
    }

    private DifyWorkflowResponse buildGenerateSuccessResponse()
    {
        DifyWorkflowResponse response = new DifyWorkflowResponse();
        DifyWorkflowResponse.WorkflowData data = new DifyWorkflowResponse.WorkflowData();
        data.setStatus("succeeded");

        DifyFormGenerateOutput output = new DifyFormGenerateOutput();
        output.setFilledFilePath("/tmp/filled.docx");
        output.setFilledValues(Map.of("姓名", "张三", "学号", "2023001001"));
        output.setTokenCost(1200);

        data.setOutputs(objectMapper.convertValue(output, Map.class));
        response.setData(data);
        return response;
    }

    private LingdocFormField buildField(String name, String type, String value)
    {
        LingdocFormField field = new LingdocFormField();
        field.setFieldName(name);
        field.setFieldType(type);
        field.setUserValue(value);
        field.setIsConfirmed("1");
        return field;
    }
}
