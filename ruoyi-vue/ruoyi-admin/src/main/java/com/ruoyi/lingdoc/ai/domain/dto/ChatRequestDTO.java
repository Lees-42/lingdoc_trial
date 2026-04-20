package com.ruoyi.lingdoc.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI对话请求DTO
 */
@Data
public class ChatRequestDTO {

    @NotBlank(message = "消息内容不能为空")
    private String content;

    /** 会话ID（新建会话可为空） */
    private String sessionId;

    /** 关联知识库ID（可选） */
    private String kbId;

    /** 父消息ID（支持分支对话） */
    private String parentMessageId;

    /** 是否使用RAG检索 */
    private Boolean useRag = true;

    /** 检索返回的最大分块数 */
    private Integer retrievalTopK = 5;

    /** 系统提示词（覆盖默认） */
    private String systemPrompt;

    /** 温度参数（0-2） */
    private Double temperature = 0.7;

    /** 最大生成token数 */
    private Integer maxTokens = 2048;
}
