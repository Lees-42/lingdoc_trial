package com.ruoyi.lingdoc.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.uuid.UUID;
import com.ruoyi.lingdoc.ai.domain.entity.KbDocumentChunk;
import com.ruoyi.lingdoc.ai.domain.entity.KbEmbedding;
import com.ruoyi.lingdoc.ai.mapper.KbDocumentChunkMapper;
import com.ruoyi.lingdoc.ai.mapper.KbEmbeddingMapper;
import com.ruoyi.lingdoc.ai.service.IEmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量化服务实现（OpenAI API）
 */
@Slf4j
@Service
public class EmbeddingServiceImpl implements IEmbeddingService {

    @Value("${lingdoc.ai.embedding.model:text-embedding-3-small}")
    private String modelName;

    @Value("${lingdoc.ai.embedding.api-key:}")
    private String apiKey;

    @Value("${lingdoc.ai.embedding.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${lingdoc.ai.embedding.dimension:1536}")
    private int dimension;

    @Autowired
    private KbEmbeddingMapper embeddingMapper;

    @Autowired
    private KbDocumentChunkMapper chunkMapper;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 模拟模式（无API Key时使用随机向量）
    private boolean mockMode = false;

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API Key未配置，启用模拟模式（随机向量）");
            mockMode = true;
        }
    }

    @Override
    public float[] embed(String text) {
        if (mockMode) {
            return generateMockEmbedding(text);
        }

        try {
            String url = baseUrl + "/embeddings";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("input", text);
            body.put("model", modelName);
            body.put("encoding_format", "float");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode embeddingNode = root.path("data").get(0).path("embedding");

                float[] embedding = new float[embeddingNode.size()];
                for (int i = 0; i < embeddingNode.size(); i++) {
                    embedding[i] = (float) embeddingNode.get(i).asDouble();
                }
                return embedding;
            } else {
                log.error("Embedding API调用失败: {}", response.getStatusCode());
                return generateMockEmbedding(text);
            }
        } catch (Exception e) {
            log.error("Embedding调用异常: {}", e.getMessage(), e);
            return generateMockEmbedding(text);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("向量维度不匹配");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    @Override
    public void saveEmbedding(KbEmbedding embedding) {
        embeddingMapper.insert(embedding);
    }

    @Override
    public int generateEmbeddingsForKb(String kbId) {
        // 查询所有未生成向量的分块
        List<KbDocumentChunk> chunks = chunkMapper.selectByKbId(kbId);
        int count = 0;

        for (KbDocumentChunk chunk : chunks) {
            // 检查是否已存在向量
            KbEmbedding existing = embeddingMapper.selectByChunkId(chunk.getChunkId());
            if (existing != null) {
                continue;
            }

            // 生成向量
            float[] vector = embed(chunk.getChunkText());

            // 保存向量
            KbEmbedding embedding = new KbEmbedding();
            embedding.setEmbeddingId("emb_" + UUID.fastUUID().toString(true));
            embedding.setChunkId(chunk.getChunkId());
            embedding.setKbId(kbId);
            embedding.setModelName(modelName);
            embedding.setVectorDimension(vector.length);
            embedding.setVectorData(floatArrayToBytes(vector));
            embedding.setNormalized(1);
            embedding.setCreatedAt(LocalDateTime.now());

            embeddingMapper.insert(embedding);
            count++;

            // 避免API限流
            if (!mockMode && count % 10 == 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        log.info("知识库向量化完成: kbId={}, count={}", kbId, count);
        return count;
    }

    /**
     * 生成模拟向量（用于测试）
     */
    private float[] generateMockEmbedding(String text) {
        float[] vector = new float[dimension];
        // 使用文本哈希作为种子生成确定性随机向量
        int seed = text.hashCode();
        java.util.Random random = new java.util.Random(seed);
        for (int i = 0; i < dimension; i++) {
            vector[i] = (float) random.nextGaussian();
        }
        // 归一化
        float norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        for (int i = 0; i < dimension; i++) {
            vector[i] /= norm;
        }
        return vector;
    }

    /**
     * float数组转字节数组
     */
    private byte[] floatArrayToBytes(float[] floats) {
        ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

    /**
     * 字节数组转float数组
     */
    public float[] bytesToFloatArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        float[] floats = new float[bytes.length / 4];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = buffer.getFloat();
        }
        return floats;
    }
}
