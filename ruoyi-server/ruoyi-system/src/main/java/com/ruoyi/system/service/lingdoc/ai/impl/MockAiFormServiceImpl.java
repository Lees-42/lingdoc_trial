package com.ruoyi.system.service.lingdoc.ai.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.lingdoc.LingdocFormField;
import com.ruoyi.system.domain.lingdoc.ai.AiExtractResult;
import com.ruoyi.system.domain.lingdoc.ai.AiGenerateResult;
import com.ruoyi.system.service.lingdoc.ai.IAiFormService;

/**
 * AI 表格填写服务 Mock 实现
 * <p>
 * 占位实现，保证 Spring 启动时不报错。
 * 另一位开发者需替换为真实的 AI 调用逻辑。
 * 
 * @author lingdoc
 */
@Service
public class MockAiFormServiceImpl implements IAiFormService
{
    /**
     * Mock 字段识别：返回示例数据
     */
    @Override
    public AiExtractResult extract(String filePath, String originalFileName)
    {
        AiExtractResult result = new AiExtractResult();
        result.setFields(new ArrayList<>());
        result.setReferences(new ArrayList<>());
        result.setTokenCost(0);
        return result;
    }

    /**
     * Mock 文档生成：方式 A —— 直接返回原始文件路径（模拟 AI 已生成填写后文件）
     * <p>
     * 真实 AI 实现应：
     * 1. 使用 python-docx / PyPDF2 / openpyxl 等库操作原始文件
     * 2. 将 confirmedFields 中的值填入对应字段位置
     * 3. 保存填写后的文件到临时路径
     * 4. 返回填写后文件的绝对路径（设置到 result.filledFilePath）
     */
    @Override
    public AiGenerateResult generate(String taskId, String originalFilePath, List<LingdocFormField> confirmedFields)
    {
        AiGenerateResult result = new AiGenerateResult();

        // 方式 A：AI 直接生成文件，返回文件路径
        // Mock 实现：直接返回原始文件路径（相当于未做任何修改，仅供流程跑通）
        result.setFilledFilePath(originalFilePath);

        // 方式 B 的字段值映射也一并填充（供 HTML 格式备用渲染）
        Map<String, String> filledValues = new HashMap<>();
        if (confirmedFields != null)
        {
            for (LingdocFormField field : confirmedFields)
            {
                String value = field.getUserValue();
                if (value == null || value.isEmpty())
                {
                    value = field.getAiValue();
                }
                filledValues.put(field.getFieldName(), value);
            }
        }
        result.setFilledValues(filledValues);
        result.setTokenCost(0);
        return result;
    }
}
