package com.ruoyi.lingdoc.ai.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档视图对象
 */
@Data
public class KbDocumentVO {

    private String docId;
    private String kbId;
    private String originalName;
    private String storageName;
    private String fileType;
    private Long fileSize;
    private String fileSizeFormatted;
    private Integer pageCount;
    private Integer charCount;
    private Integer chunkCount;
    private Integer processStatus;
    private String processStatusName;
    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
