package com.ruoyi.lingdoc.ai.service;

import com.ruoyi.lingdoc.ai.domain.entity.KbDocumentChunk;

import java.util.List;

/**
 * 文本分块服务
 */
public interface IChunkingService {

    /**
     * 对文本进行分块
     * @param text 原始文本
     * @param chunkSize 分块大小
     * @param chunkOverlap 重叠大小
     * @param docId 文档ID
     * @param kbId 知识库ID
     * @return 分块列表
     */
    List<KbDocumentChunk> chunkText(String text, int chunkSize, int chunkOverlap, String docId, String kbId);

    /**
     * 智能分块（按段落+固定长度）
     * @param text 原始文本
     * @param chunkSize 分块大小
     * @param chunkOverlap 重叠大小
     * @param docId 文档ID
     * @param kbId 知识库ID
     * @return 分块列表
     */
    List<KbDocumentChunk> smartChunk(String text, int chunkSize, int chunkOverlap, String docId, String kbId);
}
