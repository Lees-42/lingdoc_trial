package com.ruoyi.lingdoc.ai.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI会话VO
 */
@Data
public class ChatSessionVO {

    private String sessionId;
    private String kbId;
    private String kbName;
    private String sessionTitle;
    private Integer sessionType;
    private String sessionTypeName;
    private String modelName;
    private Integer messageCount;
    private Integer totalTokensIn;
    private Integer totalTokensOut;
    private Integer isPinned;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMessageAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
