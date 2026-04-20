package com.ruoyi.lingdoc.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import com.ruoyi.common.core.domain.BaseEntity;

import java.time.LocalDateTime;

/**
 * 知识库实体
 */
@Data
@TableName("kb_knowledge_base")
public class KbKnowledgeBase extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 知识库唯一标识(业务主键) */
    private String kbId;

    /** 所属用户UUID */
    private String userUuid;

    /** 知识库名称 */
    private String kbName;

    /** 知识库描述 */
    private String kbDesc;

    /** 向量模型标识 */
    private String embeddingModel;

    /** 分块大小(字符数) */
    private Integer chunkSize;

    /** 分块重叠大小 */
    private Integer chunkOverlap;

    /** 状态:0-禁用,1-启用,2-构建中 */
    private Integer status;

    /** 关联文档数量 */
    private Integer docCount;

    /** 总分块数量 */
    private Integer chunkCount;

    /** 扩展配置(JSON) */
    private String metadata;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /** 备注 */
    private String remark;

    // 非数据库字段
    /** 状态名称 */
    @TableField(exist = false)
    private String statusName;
}
