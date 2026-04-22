package com.ruoyi.system.service.lingdoc.ai;

import java.util.List;
import com.ruoyi.system.domain.lingdoc.LingdocFormField;
import com.ruoyi.system.domain.lingdoc.ai.AiExtractResult;
import com.ruoyi.system.domain.lingdoc.ai.AiGenerateResult;

/**
 * 表格填写助手 AI 服务接口
 * <p>
 * 由另一位开发者实现真实的 AI 调用逻辑。
 * 
 * @author lingdoc
 */
public interface IAiFormService
{
    /**
     * 字段识别：从空白表格中提取需要填写的字段
     * 
     * @param filePath 原始文件绝对路径
     * @param originalFileName 原始文件名
     * @return 识别结果（字段列表 + 参考文档列表）
     */
    public AiExtractResult extract(String filePath, String originalFileName);

    /**
     * 文档生成：根据已确认字段生成填写后的文档数据
     * 
     * @param taskId 任务ID
     * @param originalFilePath 原始文件绝对路径
     * @param confirmedFields 用户已确认的字段列表
     * @return 生成结果（字段值映射表）
     */
    public AiGenerateResult generate(String taskId, String originalFilePath, List<LingdocFormField> confirmedFields);
}
