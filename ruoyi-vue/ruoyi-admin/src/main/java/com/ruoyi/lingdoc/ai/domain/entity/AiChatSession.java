package com.ruoyi.lingdoc.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI会话实体
 */
@Data
@TableName("ai_chat_session")
public class AiChatSession {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话唯一标识 */
    private String sessionId;

    /** 所属用户UUID */
    private String userUuid;

    /** 关联知识库ID(可选) */
    private String kbId;

    /** 会话标题 */
    private String sessionTitle;

    /** 会话类型:1-知识库问答,2-自由对话 */
    private Integer sessionType;

    /** 使用的大模型 */
    private String modelName;

    /** 消息数量 */
    private Integer messageCount;

    /** 累计输入Token */
    private Integer totalTokensIn;

    /** 累计输出Token */
    private Integer totalTokensOut;

    /** 最后消息时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMessageAt;

    /** 是否置顶:0-否,1-是 */
    private Integer isPinned;

    /** 状态:0-已删除,1-正常 */
    private Integer status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
