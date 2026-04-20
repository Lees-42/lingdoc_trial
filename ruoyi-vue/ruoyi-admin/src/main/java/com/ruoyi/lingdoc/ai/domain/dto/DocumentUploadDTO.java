package com.ruoyi.lingdoc.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档上传请求DTO
 */
@Data
public class DocumentUploadDTO {

    @NotBlank(message = "知识库ID不能为空")
    private String kbId;

    /** 上传的文件 */
    private MultipartFile file;

    /** 是否覆盖同名文件 */
    private Boolean overwrite = false;
}
