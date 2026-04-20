package com.ruoyi.lingdoc.ai.service;

import com.ruoyi.lingdoc.ai.domain.dto.ChatRequestDTO;
import com.ruoyi.lingdoc.ai.domain.vo.ChatResponseVO;
import com.ruoyi.lingdoc.ai.domain.vo.ChatSessionVO;

import java.util.List;

/**
 * AI对话服务接口
 */
public interface IChatService {

    /**
     * 创建新会话
     */
    ChatSessionVO createSession(String kbId, String title);

    /**
     * 获取会话列表
     */
    List<ChatSessionVO> listSessions();

    /**
     * 获取会话消息历史
     */
    List<ChatMessageVO> getMessages(String sessionId);

    /**
     * 发送消息并获取AI回复（普通版）
     */
    ChatResponseVO chat(ChatRequestDTO request);

    /**
     * 发送消息并获取AI回复（流式版 - SSE）
     */
    void chatStream(ChatRequestDTO request, ChatStreamCallback callback);

    /**
     * 删除会话
     */
    boolean deleteSession(String sessionId);

    /**
     * 流式回调接口
     */
    interface ChatStreamCallback {
        void onStart();
        void onMessage(String content);
        void onComplete(ChatResponseVO response);
        void onError(String error);
    }

    /**
     * 消息VO（内部类）
     */
    class ChatMessageVO {
        private String messageId;
        private String role;
        private String content;
        private String createdAt;

        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
}
