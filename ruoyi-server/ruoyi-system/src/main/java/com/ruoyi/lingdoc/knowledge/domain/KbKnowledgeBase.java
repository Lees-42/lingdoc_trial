package com.ruoyi.lingdoc.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_knowledge_base")
public class KbKnowledgeBase extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    private String kbId;

    private String userUuid;

    private String kbName;

    private String kbDescription;

    private Integer chunkSize;

    private Integer chunkOverlap;

    private String embeddingModel;

    private Integer totalFiles;

    private Integer totalChunks;

    private String status;
}
