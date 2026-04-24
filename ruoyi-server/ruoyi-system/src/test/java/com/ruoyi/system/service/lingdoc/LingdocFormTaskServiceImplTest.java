package com.ruoyi.system.service.lingdoc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.lingdoc.LingdocFormField;
import com.ruoyi.system.domain.lingdoc.LingdocFormTask;
import com.ruoyi.system.domain.lingdoc.ai.AiGenerateResult;
import com.ruoyi.system.mapper.lingdoc.LingdocFileIndexMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocFormFieldMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocFormReferenceMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocFormTaskMapper;
import com.ruoyi.system.service.lingdoc.ai.IAiFormService;
import com.ruoyi.system.service.lingdoc.util.FormDocumentRenderer;

/**
 * 表格填写任务服务单元测试
 * <p>
 * 重点覆盖 generateDocument 方法的各种分支：
 * - 方式 A：AI 直接生成文件路径
 * - 方式 B：AI 返回 filledValues，后端 POI 渲染
 * - 异常情况：任务不存在、无确认字段等
 */
@ExtendWith(MockitoExtension.class)
class LingdocFormTaskServiceImplTest
{

    @TempDir
    File tempDir;

    @Mock
    private LingdocFormTaskMapper formTaskMapper;

    @Mock
    private LingdocFormFieldMapper formFieldMapper;

    @Mock
    private LingdocFormReferenceMapper formReferenceMapper;

    @Mock
    private LingdocFileIndexMapper lingdocFileIndexMapper;

    @Mock
    private IAiFormService aiFormService;

    @Mock
    private FormDocumentRenderer formDocumentRenderer;

    @InjectMocks
    private LingdocFormTaskServiceImpl formTaskService;

    private static final String TASK_ID = "task_test_001";
    private static final String ORIGINAL_FILE_NAME = "申请表.docx";

    @BeforeEach
    void setUp()
    {
        // Mock 静态方法
    }

    @Test
    void testGenerateDocument_WithFilledValues_PoiRender()
    {
        // 准备：任务 + 已确认字段 + AI 返回 filledValues（方式 B）
        LingdocFormTask task = buildTask();
        when(formTaskMapper.selectLingdocFormTaskById(TASK_ID)).thenReturn(task);

        LingdocFormField field1 = buildField("姓名", "text", "张三");
        LingdocFormField field2 = buildField("学号", "text", "2023001001");
        when(formFieldMapper.selectLingdocFormFieldByTaskId(TASK_ID))
            .thenReturn(Arrays.asList(field1, field2));

        AiGenerateResult aiResult = new AiGenerateResult();
        aiResult.setFilledFilePath(null); // 方式 B
        aiResult.setFilledValues(Map.of("姓名", "张三", "学号", "2023001001"));
        aiResult.setTokenCost(800);
        when(aiFormService.generate(eq(TASK_ID), anyString(), anyList())).thenReturn(aiResult);

        // Mock 静态工具
        try (MockedStatic<RuoYiConfig> ruoyi = Mockito.mockStatic(RuoYiConfig.class);
             MockedStatic<DateUtils> date = Mockito.mockStatic(DateUtils.class))
        {
            ruoyi.when(RuoYiConfig::getUploadPath).thenReturn(tempDir.getAbsolutePath());
            ruoyi.when(RuoYiConfig::getProfile).thenReturn(tempDir.getAbsolutePath());
            date.when(DateUtils::datePath).thenReturn("2026/04/25");
            date.when(DateUtils::dateTimeNow).thenReturn("20260425120000");

            // Mock 渲染器返回替换数量
            when(formDocumentRenderer.render(anyString(), anyString(), anyMap(), eq("docx")))
                .thenReturn(2);

            // 执行
            String filledPath = formTaskService.generateDocument(TASK_ID);

            // 验证
            assertNotNull(filledPath);
            assertTrue(filledPath.contains("已填写"));
            assertTrue(filledPath.endsWith(".docx"));

            // 验证任务状态更新
            verify(formTaskMapper).updateLingdocFormTask(argThat(t ->
                "3".equals(t.getStatus()) &&
                t.getFilledFileUrl() != null &&
                t.getTokenCost() == 800
            ));

            // 验证渲染器被调用
            verify(formDocumentRenderer).render(anyString(), anyString(), anyMap(), eq("docx"));
        }
    }

    @Test
    void testGenerateDocument_WithFilledFilePath_DirectCopy()
    {
        // 准备：方式 A，AI 直接返回生成好的文件路径
        LingdocFormTask task = buildTask();
        when(formTaskMapper.selectLingdocFormTaskById(TASK_ID)).thenReturn(task);

        LingdocFormField field = buildField("姓名", "text", "张三");
        when(formFieldMapper.selectLingdocFormFieldByTaskId(TASK_ID))
            .thenReturn(Collections.singletonList(field));

        // AI 返回方式 A：已有生成好的文件
        File aiGeneratedFile = new File(tempDir, "ai_filled.docx");
        try
        {
            org.apache.commons.io.FileUtils.writeStringToFile(aiGeneratedFile, "AI generated content", "UTF-8");
        }
        catch (IOException e)
        {
            fail("创建测试文件失败");
        }

        AiGenerateResult aiResult = new AiGenerateResult();
        aiResult.setFilledFilePath(aiGeneratedFile.getAbsolutePath());
        aiResult.setTokenCost(1200);
        when(aiFormService.generate(eq(TASK_ID), anyString(), anyList())).thenReturn(aiResult);

        try (MockedStatic<RuoYiConfig> ruoyi = Mockito.mockStatic(RuoYiConfig.class);
             MockedStatic<DateUtils> date = Mockito.mockStatic(DateUtils.class))
        {
            ruoyi.when(RuoYiConfig::getUploadPath).thenReturn(tempDir.getAbsolutePath());
            ruoyi.when(RuoYiConfig::getProfile).thenReturn(tempDir.getAbsolutePath());
            date.when(DateUtils::datePath).thenReturn("2026/04/25");
            date.when(DateUtils::dateTimeNow).thenReturn("20260425120000");

            // 执行
            String filledPath = formTaskService.generateDocument(TASK_ID);

            // 验证
            assertNotNull(filledPath);
            assertTrue(new File(filledPath).exists());
            // 方式 A 不调用渲染器
            verify(formDocumentRenderer, never()).render(anyString(), anyString(), anyMap(), anyString());
        }
    }

    @Test
    void testGenerateDocument_TaskNotFound()
    {
        when(formTaskMapper.selectLingdocFormTaskById(TASK_ID)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            formTaskService.generateDocument(TASK_ID);
        });

        assertTrue(ex.getMessage().contains("任务不存在"));
    }

    @Test
    void testGenerateDocument_NoConfirmedFields()
    {
        // 任务存在，但没有已确认的字段
        LingdocFormTask task = buildTask();
        when(formTaskMapper.selectLingdocFormTaskById(TASK_ID)).thenReturn(task);
        when(formFieldMapper.selectLingdocFormFieldByTaskId(TASK_ID))
            .thenReturn(Collections.emptyList());

        // AI 即使返回了结果（空值），也应该正常处理
        AiGenerateResult aiResult = new AiGenerateResult();
        aiResult.setFilledFilePath(null);
        aiResult.setFilledValues(new HashMap<>());
        aiResult.setTokenCost(0);
        when(aiFormService.generate(eq(TASK_ID), anyString(), eq(Collections.emptyList())))
            .thenReturn(aiResult);

        try (MockedStatic<RuoYiConfig> ruoyi = Mockito.mockStatic(RuoYiConfig.class);
             MockedStatic<DateUtils> date = Mockito.mockStatic(DateUtils.class))
        {
            ruoyi.when(RuoYiConfig::getUploadPath).thenReturn(tempDir.getAbsolutePath());
            ruoyi.when(RuoYiConfig::getProfile).thenReturn(tempDir.getAbsolutePath());
            date.when(DateUtils::datePath).thenReturn("2026/04/25");
            date.when(DateUtils::dateTimeNow).thenReturn("20260425120000");

            when(formDocumentRenderer.render(anyString(), anyString(), anyMap(), eq("docx")))
                .thenReturn(0);

            String filledPath = formTaskService.generateDocument(TASK_ID);

            assertNotNull(filledPath);
            // 无确认字段时，传给 AI 的是空列表
            verify(aiFormService).generate(eq(TASK_ID), anyString(), eq(Collections.emptyList()));
        }
    }

    @Test
    void testGenerateDocument_UnsupportedFormat_FallbackToRenderer()
    {
        // 原始文件是不支持的格式（如 .txt），fallback 到 renderer，抛异常后复制原文件
        LingdocFormTask task = buildTask();
        task.setOriginalFileName("备注.txt");
        // 手动构造 originalFileUrl
        task.setOriginalFileUrl("/profile/upload/2026/04/25/备注.txt");
        when(formTaskMapper.selectLingdocFormTaskById(TASK_ID)).thenReturn(task);

        LingdocFormField field = buildField("内容", "text", "测试内容");
        when(formFieldMapper.selectLingdocFormFieldByTaskId(TASK_ID))
            .thenReturn(Collections.singletonList(field));

        AiGenerateResult aiResult = new AiGenerateResult();
        aiResult.setFilledFilePath(null);
        aiResult.setFilledValues(Map.of("内容", "测试内容"));
        aiResult.setTokenCost(100);
        when(aiFormService.generate(eq(TASK_ID), anyString(), anyList())).thenReturn(aiResult);

        // 创建原始文件
        File originalFile = new File(tempDir, "upload/2026/04/25/备注.txt");
        originalFile.getParentFile().mkdirs();
        try
        {
            org.apache.commons.io.FileUtils.writeStringToFile(originalFile, "原始内容", "UTF-8");
        }
        catch (IOException e)
        {
            fail("创建测试文件失败");
        }

        try (MockedStatic<RuoYiConfig> ruoyi = Mockito.mockStatic(RuoYiConfig.class);
             MockedStatic<DateUtils> date = Mockito.mockStatic(DateUtils.class))
        {
            ruoyi.when(RuoYiConfig::getUploadPath).thenReturn(tempDir.getAbsolutePath());
            ruoyi.when(RuoYiConfig::getProfile).thenReturn(tempDir.getAbsolutePath());
            date.when(DateUtils::datePath).thenReturn("2026/04/25");
            date.when(DateUtils::dateTimeNow).thenReturn("20260425120000");

            // 渲染器抛 UnsupportedOperationException，fallback 复制原文件
            when(formDocumentRenderer.render(anyString(), anyString(), anyMap(), eq("txt")))
                .thenThrow(new UnsupportedOperationException("不支持 txt"));

            String filledPath = formTaskService.generateDocument(TASK_ID);

            assertNotNull(filledPath);
            assertTrue(filledPath.endsWith(".txt"));
            // 验证渲染器被调用（虽然失败了）
            verify(formDocumentRenderer).render(anyString(), anyString(), anyMap(), eq("txt"));
        }
    }

    // ========== 辅助方法 ==========

    private LingdocFormTask buildTask()
    {
        LingdocFormTask task = new LingdocFormTask();
        task.setTaskId(TASK_ID);
        task.setOriginalFileName(ORIGINAL_FILE_NAME);
        task.setOriginalFileUrl("/profile/upload/2026/04/25/申请表.docx");
        task.setStatus("2");
        task.setTokenCost(0);
        return task;
    }

    private LingdocFormField buildField(String name, String type, String value)
    {
        LingdocFormField field = new LingdocFormField();
        field.setFieldId("f_" + name);
        field.setFieldName(name);
        field.setFieldType(type);
        field.setUserValue(value);
        field.setIsConfirmed("1");
        return field;
    }
}
