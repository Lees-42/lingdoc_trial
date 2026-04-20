package com.ruoyi.lingdoc.ai.service;

import com.ruoyi.lingdoc.ai.domain.entity.KbEmbedding;

import java.util.List;

/**
 * 向量化服务接口
 */
public interface IEmbeddingService {

    /**
     * 将文本转换为向量
     * @param text 输入文本
     * @return 向量数组（float[]）
     */
    float[] embed(String text);

    /**
     * 批量向量化
     * @param texts 文本列表
     * @return 向量列表
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 获取向量维度
     */
    int getDimension();

    /**
     * 获取模型名称
     */
    String getModelName();

    /**
     * 计算余弦相似度
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 相似度（-1到1）
     */
    double cosineSimilarity(float[] vec1, float[] vec2);

    /**
     * 保存向量到数据库
     */
    void saveEmbedding(KbEmbedding embedding);

    /**
     * 为知识库的所有分块生成向量
     */
    int generateEmbeddingsForKb(String kbId);
}
