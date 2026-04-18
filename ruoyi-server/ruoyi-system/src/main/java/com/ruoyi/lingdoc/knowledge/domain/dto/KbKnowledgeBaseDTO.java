package com.ruoyi.lingdoc.knowledge.domain.dto;

import com.ruoyi.common.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KbKnowledgeBaseDTO extends PageQuery {

    private static final long serialVersionUID = 1L;

    private String kbId;

    private String kbName;

    private String kbDescription;

    private Integer chunkSize;

    private Integer chunkOverlap;

    private String embeddingModel;

    private String status;
}
