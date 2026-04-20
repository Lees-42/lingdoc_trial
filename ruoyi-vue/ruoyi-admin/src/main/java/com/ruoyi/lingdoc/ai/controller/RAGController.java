package com.ruoyi.lingdoc.ai.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.lingdoc.ai.service.IEmbeddingService;
import com.ruoyi.lingdoc.ai.service.IRAGService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG检索Controller
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/ai/rag")
public class RAGController extends BaseController {

    @Autowired
    private IEmbeddingService embeddingService;

    @Autowired
    private IRAGService ragService;

    /**
     * 测试文本向量化
     */
    @PostMapping("/embed")
    public AjaxResult embed(@RequestParam String text) {
        log.info("测试向量化: text={}", text.substring(0, Math.min(50, text.length())));
        float[] vector = embeddingService.embed(text);
        return success(Map.of(
            "dimension", vector.length,
            "sample", vector.length > 0 ? vector[0] : 0,
            "model", embeddingService.getModelName()
        ));
    }

    /**
     * 为知识库生成向量
     */
    @PostMapping("/generate/{kbId}")
    public AjaxResult generateEmbeddings(@PathVariable String kbId) {
        log.info("生成知识库向量: kbId={}", kbId);
        int count = embeddingService.generateEmbeddingsForKb(kbId);
        return success(Map.of("generatedCount", count));
    }

    /**
     * RAG检索测试
     */
    @PostMapping("/search/{kbId}")
    public AjaxResult search(@PathVariable String kbId,
                            @RequestParam String query,
                            @RequestParam(defaultValue = "5") int topK) {
        log.info("RAG检索: kbId={}, query={}", kbId, query);
        List<IRAGService.ScoredChunk> results = ragService.hybridSearch(kbId, query, topK);
        
        List<Object> simplified = results.stream().map(r -> Map.of(
            "chunkId", r.getChunk().getChunkId(),
            "score", r.getScore(),
            "searchType", r.getSearchType(),
            "textPreview", r.getChunk().getChunkText().substring(0, Math.min(100, r.getChunk().getChunkText().length())) + "..."
        )).collect(Collectors.toList());
        
        return success(simplified);
    }


}
