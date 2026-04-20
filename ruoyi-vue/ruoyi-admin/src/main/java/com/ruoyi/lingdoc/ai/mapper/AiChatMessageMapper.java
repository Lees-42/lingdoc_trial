package com.ruoyi.lingdoc.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.lingdoc.ai.domain.entity.AiChatMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 会话消息Mapper
 */
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {

    /**
     * 根据会话ID查询消息列表
     */
    @Select("SELECT * FROM ai_chat_message WHERE session_id = #{sessionId} AND status = 1 ORDER BY created_at ASC")
    List<AiChatMessage> selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 获取会话最新消息
     */
    @Select("SELECT * FROM ai_chat_message WHERE session_id = #{sessionId} AND status = 1 ORDER BY created_at DESC LIMIT 1")
    AiChatMessage selectLastMessage(@Param("sessionId") String sessionId);
}
