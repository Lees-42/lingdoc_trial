package com.ruoyi.lingdoc.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新知识库请求DTO
 */
@Data
public class KbUpdateDTO {
    
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 100, message = "名称长度不能超过100字符")
    private String kbName;
    
    @Size(max = 500, message = "描述长度不能超过500字符")
    private String kbDesc;
    
    private Integer chunkSize;
    
    private Integer chunkOverlap;
}