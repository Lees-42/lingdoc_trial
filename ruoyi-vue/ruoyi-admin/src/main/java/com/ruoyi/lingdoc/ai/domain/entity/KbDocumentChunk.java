package com.ruoyi.lingdoc.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档分块实体
 */
@Data
@TableName("kb_document_chunk")
public class KbDocumentChunk {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分块唯一标识 */
    private String chunkId;

    /** 所属知识库ID */
    private String kbId;

    /** 来源文件ID(关联file_index) */
    private String fileId;

    /** 分块序号(同一文档内递增) */
    private Integer chunkIndex;

    /** 分块文本内容 */
    private String chunkText;

    /** 文本长度(字符数) */
    private Integer chunkTextLength;

    /** 在原文档中的起始字符位置 */
    private Integer charStart;

    /** 在原文档中的结束字符位置 */
    private Integer charEnd;

    /** 页码(PDF/Word文档适用) */
    private Integer pageNumber;

    /** 层级标题路径(JSON) */
    private String headingHierarchy;

    /** 关键词数组(JSON) */
    private String keywords;

    /** 状态:0-无效,1-有效,2-待索引 */
    private Integer status;

    /** 扩展元数据(JSON) */
    private String metadata;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
