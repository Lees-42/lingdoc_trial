package com.ruoyi.lingdoc.ai.service.impl;

import com.ruoyi.lingdoc.ai.domain.entity.KbDocumentChunk;
import com.ruoyi.lingdoc.ai.domain.entity.KbEmbedding;
import com.ruoyi.lingdoc.ai.mapper.KbDocumentChunkMapper;
import com.ruoyi.lingdoc.ai.mapper.KbEmbeddingMapper;
import com.ruoyi.lingdoc.ai.service.IEmbeddingService;
import com.ruoyi.lingdoc.ai.service.IRAGService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG检索服务实现
 */
@Slf4j
@Service
public class RAGServiceImpl implements IRAGService {

    @Autowired
    private IEmbeddingService embeddingService;

    @Autowired
    private KbEmbeddingMapper embeddingMapper;

    @Autowired
    private KbDocumentChunkMapper chunkMapper;

    @Value("${lingdoc.ai.retrieval.top-k:5}")
    private int defaultTopK;

    @Value("${lingdoc.ai.retrieval.min-score:0.75}")
    private double minScore;

    // 融合权重：向量检索 vs 关键词检索
    private static final double VECTOR_WEIGHT = 0.7;
    private static final double KEYWORD_WEIGHT = 0.3;

    @Override
    public List<ScoredChunk> vectorSearch(String kbId, String query, int topK) {
        // 1. 查询向量化
        float[] queryVector = embeddingService.embed(query);

        // 2. 获取知识库所有向量
        List<KbEmbedding> embeddings = embeddingMapper.selectByKbId(kbId);

        // 3. 计算相似度并排序
        List<ScoredChunk> results = new ArrayList<>();
        for (KbEmbedding emb : embeddings) {
            float[] docVector = bytesToFloatArray(emb.getVectorData());
            double similarity = embeddingService.cosineSimilarity(queryVector, docVector);

            // 过滤低相似度
            if (similarity >= minScore) {
                KbDocumentChunk chunk = chunkMapper.selectByChunkId(emb.getChunkId());
                if (chunk != null && chunk.getStatus() == 1) {
                    results.add(new ScoredChunk(chunk, similarity, "vector"));
                }
            }
        }

        // 4. 按相似度排序并取topK
        return results.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    @Override
    public List<KbDocumentChunk> keywordSearch(String kbId, String keywords, int limit) {
        // 使用MySQL全文索引
        return chunkMapper.fullTextSearch(kbId, keywords, limit);
    }

    @Override
    public List<ScoredChunk> hybridSearch(String kbId, String query, int topK) {
        int singleTopK = topK * 2; // 每种检索多取一些用于融合

        // 1. 向量检索
        List<ScoredChunk> vectorResults = vectorSearch(kbId, query, singleTopK);

        // 2. 关键词检索
        List<KbDocumentChunk> keywordResults = keywordSearch(kbId, query, singleTopK);

        // 3. 融合排序（RRF - Reciprocal Rank Fusion）
        Map<String, ScoredChunk> fusionMap = new HashMap<>();
        Map<String, Integer> vectorRankMap = new HashMap<>();
        Map<String, Integer> keywordRankMap = new HashMap<>();

        // 记录向量排名
        for (int i = 0; i < vectorResults.size(); i++) {
            String chunkId = vectorResults.get(i).getChunk().getChunkId();
            fusionMap.put(chunkId, vectorResults.get(i));
            vectorRankMap.put(chunkId, i + 1);
        }

        // 记录关键词排名
        for (int i = 0; i < keywordResults.size(); i++) {
            String chunkId = keywordResults.get(i).getChunkId();
            keywordRankMap.put(chunkId, i + 1);
            if (!fusionMap.containsKey(chunkId)) {
                fusionMap.put(chunkId, new ScoredChunk(keywordResults.get(i), 0.5, "keyword"));
            }
        }

        // RRF融合公式: score = Σ 1/(k + rank)
        final int k = 60; // RRF常数
        List<ScoredChunk> fusedResults = new ArrayList<>();

        for (Map.Entry<String, ScoredChunk> entry : fusionMap.entrySet()) {
            String chunkId = entry.getKey();
            KbDocumentChunk chunk = entry.getValue().getChunk();

            int vectorRank = vectorRankMap.getOrDefault(chunkId, Integer.MAX_VALUE);
            int keywordRank = keywordRankMap.getOrDefault(chunkId, Integer.MAX_VALUE);

            // 计算RRF分数
            double rrfScore = 1.0 / (k + vectorRank) + 1.0 / (k + keywordRank);

            fusedResults.add(new ScoredChunk(chunk, rrfScore, 
                vectorRank < Integer.MAX_VALUE && keywordRank < Integer.MAX_VALUE ? "hybrid" :
                vectorRank < Integer.MAX_VALUE ? "vector" : "keyword"));
        }

        // 按RRF分数排序并取topK
        return fusedResults.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    @Override
    public String buildContext(List<ScoredChunk> chunks, int maxLength) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        int currentLength = 0;

        for (ScoredChunk scored : chunks) {
            KbDocumentChunk chunk = scored.getChunk();
            String text = chunk.getChunkText();

            // 检查是否超过最大长度
            if (currentLength + text.length() > maxLength) {
                // 尝试截取
                int remaining = maxLength - currentLength;
                if (remaining > 100) {
                    context.append(text, 0, remaining);
                    context.append("...\n\n");
                }
                break;
            }

            // 添加上下文块
            context.append("【参考片段").append(scored.getChunk().getChunkIndex() + 1).append("】\n");
            context.append(text).append("\n\n");
            currentLength += text.length() + 20; // +20 for marker
        }

        return context.toString().trim();
    }

    /**
     * 字节数组转float数组
     */
    private float[] bytesToFloatArray(byte[] bytes) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        float[] floats = new float[bytes.length / 4];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = buffer.getFloat();
        }
        return floats;
    }
}
