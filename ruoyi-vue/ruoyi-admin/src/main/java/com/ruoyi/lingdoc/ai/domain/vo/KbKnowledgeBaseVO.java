package com.ruoyi.lingdoc.ai.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库视图对象
 */
@Data
public class KbKnowledgeBaseVO {
    
    private String kbId;
    private String kbName;
    private String kbDesc;
    private String embeddingModel;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Integer status;
    private String statusName;
    private Integer docCount;
    private Integer chunkCount;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}