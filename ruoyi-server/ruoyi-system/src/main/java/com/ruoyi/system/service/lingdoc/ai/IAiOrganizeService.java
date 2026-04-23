package com.ruoyi.system.service.lingdoc.ai;

import com.ruoyi.system.service.lingdoc.ai.result.AiOrganizeResult;

/**
 * 自动规整 AI 服务接口
 * <p>
 * 入参：文件路径 + 文件名 + 内容（文本）+ 用户上下文（已有目录结构、标签体系）。
 * 出参：分类建议 + 标签建议 + 重命名建议 + 摘要 + 关键词。
 * <p>
 * 另一位开发者实现真实 AI 调用（LLM / 本地模型 / 规则引擎）。
 * 
 * @author lingdoc
 */
public interface IAiOrganizeService
{
    /**
     * 对单个文件进行 AI 自动规整分析
     *
     * @param fileId      文件ID（inbox_id）
     * @param filePath    文件绝对路径
     * @param fileName    原始文件名
     * @param fileContent 文本内容（非文本类型为 null）
     * @param userId      当前用户ID
     * @return 规整建议结果
     */
    public AiOrganizeResult organize(String fileId, String filePath, String fileName,
                                     String fileContent, Long userId);
}
