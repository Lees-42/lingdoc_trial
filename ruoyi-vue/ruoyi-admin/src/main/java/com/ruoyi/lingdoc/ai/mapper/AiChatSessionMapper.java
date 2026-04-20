package com.ruoyi.lingdoc.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.lingdoc.ai.domain.entity.AiChatSession;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI会话Mapper
 */
public interface AiChatSessionMapper extends BaseMapper<AiChatSession> {

    /**
     * 根据用户UUID查询会话列表
     */
    @Select("SELECT * FROM ai_chat_session WHERE user_uuid = #{userUuid} AND status = 1 ORDER BY is_pinned DESC, last_message_at DESC")
    List<AiChatSession> selectByUserUuid(@Param("userUuid") String userUuid);

    /**
     * 根据sessionId查询会话
     */
    @Select("SELECT * FROM ai_chat_session WHERE session_id = #{sessionId} AND status = 1")
    AiChatSession selectBySessionId(@Param("sessionId") String sessionId);
}
