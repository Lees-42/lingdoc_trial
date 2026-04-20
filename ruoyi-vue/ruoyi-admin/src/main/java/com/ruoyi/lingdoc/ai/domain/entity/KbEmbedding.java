package com.ruoyi.lingdoc.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 向量嵌入实体
 */
@Data
@TableName("kb_embedding")
public class KbEmbedding {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 向量唯一标识 */
    private String embeddingId;

    /** 关联分块ID */
    private String chunkId;

    /** 所属知识库ID */
    private String kbId;

    /** 向量模型名称 */
    private String modelName;

    /** 向量维度 */
    private Integer vectorDimension;

    /** 向量数据(二进制存储) */
    private byte[] vectorData;

    /** 是否已归一化:0-否,1-是 */
    private Integer normalized;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
