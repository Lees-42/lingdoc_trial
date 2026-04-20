package com.ruoyi.lingdoc.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话消息实体
 */
@Data
@TableName("ai_chat_message")
public class AiChatMessage {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息唯一标识 */
    private String messageId;

    /** 所属会话ID */
    private String sessionId;

    /** 父消息ID(支持分支对话) */
    private String parentMessageId;

    /** 角色:1-用户,2-AI助手,3-系统 */
    private Integer role;

    /** 消息内容 */
    private String content;

    /** 内容类型:text/markdown/image */
    private String contentType;

    /** 输入Token数 */
    private Integer tokensIn;

    /** 输出Token数 */
    private Integer tokensOut;

    /** 响应延迟(毫秒) */
    private Integer latencyMs;

    /** 实际使用模型 */
    private String modelName;

    /** 关联知识库ID */
    private String kbId;

    /** 检索引用的分块ID数组(JSON) */
    private String retrievalChunks;

    /** 检索引用的文件ID数组(JSON) */
    private String retrievalFiles;

    /** 用户反馈:1-点赞,2-点踩 */
    private Integer feedbackRating;

    /** 反馈备注 */
    private String feedbackComment;

    /** 状态:0-已删除,1-正常 */
    private Integer status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
