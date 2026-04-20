package com.ruoyi.lingdoc.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档文件实体
 */
@Data
@TableName("kb_document")
public class KbDocument {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文档唯一标识 */
    private String docId;

    /** 所属知识库ID */
    private String kbId;

    /** 所属用户UUID */
    private String userUuid;

    /** 原始文件名 */
    private String originalName;

    /** 存储文件名 */
    private String storageName;

    /** 文件类型:pdf,word,excel,image,text */
    private String fileType;

    /** 文件MIME类型 */
    private String mimeType;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 存储路径 */
    private String storagePath;

    /** 文件摘要(MD5) */
    private String fileHash;

    /** 文档总页数(如适用) */
    private Integer pageCount;

    /** 文档总字符数 */
    private Integer charCount;

    /** 处理状态:0-待处理,1-处理中,2-已完成,3-失败 */
    private Integer processStatus;

    /** 处理错误信息 */
    private String errorMsg;

    /** 分块数量 */
    private Integer chunkCount;

    /** 扩展元数据(JSON) */
    private String metadata;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
