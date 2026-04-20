package com.ruoyi.lingdoc.ai.service;

/**
 * 文档文本提取服务
 */
public interface ITextExtractionService {

    /**
     * 从文件路径提取文本内容
     * @param filePath 文件存储路径
     * @param fileType 文件类型
     * @return 提取的纯文本内容
     */
    String extractText(String filePath, String fileType);

    /**
     * 检测文件类型
     * @param filePath 文件路径
     * @return MIME类型
     */
    String detectMimeType(String filePath);
}
