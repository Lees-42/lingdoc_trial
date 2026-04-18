package com.ruoyi.lingdoc.knowledge.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库VO
 */
@Data
public class KbKnowledgeBaseVO {

    private static final long serialVersionUID = 1L;

    private String kbId;

    private String kbName;

    private String kbDescription;

    private Integer chunkSize;

    private Integer chunkOverlap;

    private String embeddingModel;

    private Integer totalFiles;

    private Integer totalChunks;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
