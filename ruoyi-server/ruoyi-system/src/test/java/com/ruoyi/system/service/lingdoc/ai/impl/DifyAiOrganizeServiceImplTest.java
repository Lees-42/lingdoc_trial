package com.ruoyi.system.service.lingdoc.ai.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.system.domain.lingdoc.LingdocTag;
import com.ruoyi.system.service.lingdoc.ILingdocTagService;
import com.ruoyi.system.service.lingdoc.ILingdocUserRepoService;
import com.ruoyi.system.service.lingdoc.ai.dify.DifyOrganizeOutput;
import com.ruoyi.system.service.lingdoc.ai.dify.DifyWorkflowClient;
import com.ruoyi.system.service.lingdoc.ai.dify.DifyWorkflowResponse;
import com.ruoyi.system.service.lingdoc.ai.result.AiOrganizeResult;

/**
 * Dify 自动规整服务单元测试
 */
class DifyAiOrganizeServiceImplTest
{
    @Mock
    private DifyWorkflowClient difyClient;

    @Mock
    private ILingdocUserRepoService userRepoService;

    @Mock
    private ILingdocTagService tagService;

    @InjectMocks
    private DifyAiOrganizeServiceImpl organizeService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        organizeService.objectMapper = objectMapper;
    }

    @Test
    void testOrganizeSuccess()
    {
        DifyWorkflowResponse response = buildSuccessResponse();
        when(difyClient.runWorkflow(anyMap(), anyString())).thenReturn(response);
        when(userRepoService.getUserRepoPath(anyLong())).thenReturn("/tmp/vault");
        when(tagService.selectLingdocTagList(any(LingdocTag.class))).thenReturn(Arrays.asList());

        AiOrganizeResult result = organizeService.organize("f_001", "/tmp/test.docx", "test.docx", "content", 1L);

        assertNotNull(result);
        assertNotNull(result.getCategory());
        assertEquals("工作/求职材料", result.getCategory().getSuggestedSubPath());
        assertNotNull(result.getRename());
        assertEquals("_参考-个人简历.docx", result.getRename().getSuggestedName());
        assertEquals(1, result.getTags().size());
        assertEquals("简历", result.getTags().get(0).getTagName());
        assertEquals("这是一份个人简历...", result.getSummary());
        assertEquals(153, result.getTokenCost());
    }

    @Test
    void testOrganizeDifyFailed()
    {
        DifyWorkflowResponse response = new DifyWorkflowResponse();
        DifyWorkflowResponse.WorkflowData data = new DifyWorkflowResponse.WorkflowData();
        data.setStatus("failed");
        response.setData(data);

        when(difyClient.runWorkflow(anyMap(), anyString())).thenReturn(response);
        when(userRepoService.getUserRepoPath(anyLong())).thenReturn("/tmp/vault");
        when(tagService.selectLingdocTagList(any(LingdocTag.class))).thenReturn(Arrays.asList());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            organizeService.organize("f_001", "/tmp/test.docx", "test.docx", "content", 1L);
        });

        assertTrue(exception.getMessage().contains("Dify 工作流执行失败"));
    }

    @Test
    void testOrganizeNullResponse()
    {
        when(difyClient.runWorkflow(anyMap(), anyString())).thenReturn(null);
        when(userRepoService.getUserRepoPath(anyLong())).thenReturn("/tmp/vault");
        when(tagService.selectLingdocTagList(any(LingdocTag.class))).thenReturn(Arrays.asList());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            organizeService.organize("f_001", "/tmp/test.docx", "test.docx", "content", 1L);
        });

        assertTrue(exception.getMessage().contains("Dify 返回空响应"));
    }

    @Test
    void testFallbacks()
    {
        DifyWorkflowResponse response = new DifyWorkflowResponse();
        DifyWorkflowResponse.WorkflowData data = new DifyWorkflowResponse.WorkflowData();
        data.setStatus("succeeded");
        data.setOutputs(new HashMap<>());
        response.setData(data);

        when(difyClient.runWorkflow(anyMap(), anyString())).thenReturn(response);
        when(userRepoService.getUserRepoPath(anyLong())).thenReturn("/tmp/vault");
        when(tagService.selectLingdocTagList(any(LingdocTag.class))).thenReturn(Arrays.asList());

        AiOrganizeResult result = organizeService.organize("f_001", "/tmp/test.docx", "original.docx", "content", 1L);

        assertNotNull(result);
        assertNotNull(result.getCategory());
        assertEquals("/", result.getCategory().getSuggestedSubPath());
        assertNotNull(result.getRename());
        assertEquals("original.docx", result.getRename().getSuggestedName());
        assertNotNull(result.getTags());
        assertTrue(result.getTags().isEmpty());
        assertNotNull(result.getKeywords());
        assertTrue(result.getKeywords().isEmpty());
        assertEquals(BigDecimal.ZERO, result.getConfidence());
        assertEquals(0, result.getTokenCost());
    }

    private DifyWorkflowResponse buildSuccessResponse()
    {
        DifyWorkflowResponse response = new DifyWorkflowResponse();
        DifyWorkflowResponse.WorkflowData data = new DifyWorkflowResponse.WorkflowData();
        data.setStatus("succeeded");

        DifyOrganizeOutput output = new DifyOrganizeOutput();
        output.setSuggestedSubPath("工作/求职材料");
        output.setReason("Detected keywords related to 'resume'.");
        output.setConfidence(new BigDecimal("0.8"));
        output.setSuggestedName("_参考-个人简历.docx");
        output.setRenameReason("Standardized filename format.");
        output.setRenameConfidence(new BigDecimal("0.9"));
        output.setSummary("这是一份个人简历...");
        output.setKeywords(Arrays.asList("简历", "求职"));
        output.setTokenCost(153L);

        DifyOrganizeOutput.DifyTagOutput tagOutput = new DifyOrganizeOutput.DifyTagOutput();
        tagOutput.setTagName("简历");
        tagOutput.setTagColor("#409EFF");
        tagOutput.setReason("Auto-tagged based on content analysis.");
        tagOutput.setConfidence(new BigDecimal("0.9"));
        output.setTags(Arrays.asList(tagOutput));

        // 将 DifyOrganizeOutput 转为 Map，模拟 RestTemplate 反序列化后的结果
        Map<String, Object> outputsMap = objectMapper.convertValue(output, Map.class);
        data.setOutputs(outputsMap);
        response.setData(data);
        return response;
    }
}
