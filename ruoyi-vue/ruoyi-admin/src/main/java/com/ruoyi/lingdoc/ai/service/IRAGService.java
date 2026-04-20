package com.ruoyi.lingdoc.ai.service;

import com.ruoyi.lingdoc.ai.domain.entity.KbDocumentChunk;

import java.util.List;

/**
 * RAG检索服务接口
 */
public interface IRAGService {

    /**
     * 语义检索：基于向量相似度
     * @param kbId 知识库ID
     * @param query 查询文本
     * @param topK 返回数量
     * @return 检索到的分块列表（带相似度分数）
     */
    List<ScoredChunk> vectorSearch(String kbId, String query, int topK);

    /**
     * 关键词检索：基于全文索引
     * @param kbId 知识库ID
     * @param keywords 关键词
     * @param limit 返回数量
     * @return 检索到的分块列表
     */
    List<KbDocumentChunk> keywordSearch(String kbId, String keywords, int limit);

    /**
     * 混合检索：向量+关键词融合
     * @param kbId 知识库ID
     * @param query 查询文本
     * @param topK 返回数量
     * @return 融合后的分块列表
     */
    List<ScoredChunk> hybridSearch(String kbId, String query, int topK);

    /**
     * 组装RAG上下文
     * @param chunks 检索到的分块
     * @param maxLength 最大上下文长度
     * @return 组装后的上下文文本
     */
    String buildContext(List<ScoredChunk> chunks, int maxLength);

    /**
     * 带分数的检索结果
     */
    class ScoredChunk {
        private KbDocumentChunk chunk;
        private double score;
        private String searchType; // "vector" | "keyword" | "hybrid"

        public ScoredChunk(KbDocumentChunk chunk, double score, String searchType) {
            this.chunk = chunk;
            this.score = score;
            this.searchType = searchType;
        }

        // Getters
        public KbDocumentChunk getChunk() { return chunk; }
        public double getScore() { return score; }
        public String getSearchType() { return searchType; }
    }
}
