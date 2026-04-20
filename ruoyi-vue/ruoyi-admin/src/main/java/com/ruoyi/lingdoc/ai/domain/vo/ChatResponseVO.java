package com.ruoyi.lingdoc.ai.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI对话响应VO
 */
@Data
public class ChatResponseVO {

    private String messageId;
    private String sessionId;
    private String role = "assistant";
    private String content;

    /** 使用的模型 */
    private String modelName;

    /** 输入token数 */
    private Integer tokensIn;

    /** 输出token数 */
    private Integer tokensOut;

    /** 响应延迟（毫秒） */
    private Integer latencyMs;

    /** 是否使用RAG */
    private Boolean usedRag;

    /** 检索到的参考文档 */
    private List<RetrievedSourceVO> sources;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 检索来源VO
     */
    @Data
    public static class RetrievedSourceVO {
        private String docId;
        private String docName;
        private String chunkText;
        private Double relevanceScore;
    }
}
