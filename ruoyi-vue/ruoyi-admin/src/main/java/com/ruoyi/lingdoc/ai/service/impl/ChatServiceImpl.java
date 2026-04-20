package com.ruoyi.lingdoc.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.bean.BeanUtils;
import com.ruoyi.common.utils.uuid.UUID;
import com.ruoyi.lingdoc.ai.domain.dto.ChatRequestDTO;
import com.ruoyi.lingdoc.ai.domain.entity.AiChatMessage;
import com.ruoyi.lingdoc.ai.domain.entity.AiChatSession;
import com.ruoyi.lingdoc.ai.domain.vo.ChatResponseVO;
import com.ruoyi.lingdoc.ai.domain.vo.ChatSessionVO;
import com.ruoyi.lingdoc.ai.mapper.AiChatMessageMapper;
import com.ruoyi.lingdoc.ai.mapper.AiChatSessionMapper;
import com.ruoyi.lingdoc.ai.mapper.KbKnowledgeBaseMapper;
import com.ruoyi.lingdoc.ai.service.IChatService;
import com.ruoyi.lingdoc.ai.service.IRAGService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI对话服务实现
 */
@Slf4j
@Service
public class ChatServiceImpl extends ServiceImpl<AiChatSessionMapper, AiChatSession> implements IChatService {

    @Value("${lingdoc.ai.llm.model:gpt-4o-mini}")
    private String modelName;

    @Value("${lingdoc.ai.llm.api-key:}")
    private String apiKey;

    @Value("${lingdoc.ai.llm.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${lingdoc.ai.llm.temperature:0.7}")
    private double temperature;

    @Value("${lingdoc.ai.llm.max-tokens:2048}")
    private int maxTokens;

    @Autowired
    private AiChatMessageMapper messageMapper;

    @Autowired
    private KbKnowledgeBaseMapper kbMapper;

    @Autowired
    private IRAGService ragService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 模拟模式（无API Key时返回模拟回复）
    private boolean mockMode = false;

    @javax.annotation.PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API Key未配置，启用模拟模式");
            mockMode = true;
        }
    }

    @Override
    public ChatSessionVO createSession(String kbId, String title) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }

        // 验证知识库
        if (kbId != null && !kbId.isEmpty()) {
            var kb = kbMapper.selectByKbId(kbId);
            if (kb == null) {
                throw new IllegalArgumentException("知识库不存在");
            }
        }

        AiChatSession session = new AiChatSession();
        session.setSessionId("chat_" + UUID.fastUUID().toString(true));
        session.setUserUuid(loginUser.getUserId().toString());
        session.setKbId(kbId);
        session.setSessionTitle(title != null ? title : "新对话");
        session.setSessionType(kbId != null ? 1 : 2); // 1-知识库问答, 2-自由对话
        session.setModelName(modelName);
        session.setMessageCount(0);
        session.setTotalTokensIn(0);
        session.setTotalTokensOut(0);
        session.setIsPinned(0);
        session.setStatus(1);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());

        this.save(session);
        log.info("创建会话: sessionId={}, user={}", session.getSessionId(), loginUser.getUserId());

        return convertToVO(session);
    }

    @Override
    public List<ChatSessionVO> listSessions() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }

        List<AiChatSession> list = baseMapper.selectByUserUuid(loginUser.getUserId().toString());
        return list.stream()
                .filter(s -> s.getStatus() == 1)
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageVO> getMessages(String sessionId) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }

        // 验证会话归属
        AiChatSession session = baseMapper.selectBySessionId(sessionId);
        if (session == null || !session.getUserUuid().equals(loginUser.getUserId().toString())) {
            throw new SecurityException("无权访问该会话");
        }

        List<AiChatMessage> messages = messageMapper.selectBySessionId(sessionId);
        return messages.stream()
                .filter(m -> m.getStatus() == 1)
                .map(this::convertToMessageVO)
                .collect(Collectors.toList());
    }

    @Override
    public ChatResponseVO chat(ChatRequestDTO request) {
        long startTime = System.currentTimeMillis();
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }

        // 获取或创建会话
        String sessionId = request.getSessionId();
        AiChatSession session;
        if (sessionId == null || sessionId.isEmpty()) {
            session = createSessionEntity(request.getKbId(), request.getContent().substring(0, Math.min(20, request.getContent().length())));
            sessionId = session.getSessionId();
        } else {
            session = baseMapper.selectBySessionId(sessionId);
            if (session == null || !session.getUserUuid().equals(loginUser.getUserId().toString())) {
                throw new SecurityException("会话不存在或无权访问");
            }
        }

        // 保存用户消息
        AiChatMessage userMessage = saveUserMessage(sessionId, request.getContent(), request.getParentMessageId());

        // RAG检索（如果启用）
        String context = "";
        List<IRAGService.ScoredChunk> retrievedChunks = new ArrayList<>();
        if (request.getUseRag() && (request.getKbId() != null || session.getKbId() != null)) {
            String kbId = request.getKbId() != null ? request.getKbId() : session.getKbId();
            int topK = request.getRetrievalTopK() != null ? request.getRetrievalTopK() : 5;
            retrievedChunks = ragService.hybridSearch(kbId, request.getContent(), topK);
            context = ragService.buildContext(retrievedChunks, 4000);
        }

        // 构建提示词
        String systemPrompt = buildSystemPrompt(request.getSystemPrompt(), context);

        // 调用LLM
        String aiContent;
        int tokensIn = 0, tokensOut = 0;

        if (mockMode) {
            aiContent = generateMockResponse(request.getContent(), context);
            tokensIn = request.getContent().length() / 4;
            tokensOut = aiContent.length() / 4;
        } else {
            LLMResponse llmResponse = callLLM(systemPrompt, request.getContent(), request.getTemperature(), request.getMaxTokens());
            aiContent = llmResponse.getContent();
            tokensIn = llmResponse.getTokensIn();
            tokensOut = llmResponse.getTokensOut();
        }

        int latency = (int) (System.currentTimeMillis() - startTime);

        // 保存AI回复
        AiChatMessage aiMessage = saveAiMessage(sessionId, userMessage.getMessageId(), aiContent, 
                tokensIn, tokensOut, latency, modelName, retrievedChunks);

        // 更新会话统计
        updateSessionStats(session, aiMessage.getCreatedAt());

        // 构建响应
        ChatResponseVO response = new ChatResponseVO();
        response.setMessageId(aiMessage.getMessageId());
        response.setSessionId(sessionId);
        response.setContent(aiContent);
        response.setModelName(modelName);
        response.setTokensIn(tokensIn);
        response.setTokensOut(tokensOut);
        response.setLatencyMs(latency);
        response.setUsedRag(request.getUseRag() && !retrievedChunks.isEmpty());
        response.setCreatedAt(aiMessage.getCreatedAt());

        // 添加检索来源
        if (!retrievedChunks.isEmpty()) {
            List<ChatResponseVO.RetrievedSourceVO> sources = new ArrayList<>();
            for (IRAGService.ScoredChunk chunk : retrievedChunks) {
                ChatResponseVO.RetrievedSourceVO source = new ChatResponseVO.RetrievedSourceVO();
                source.setDocId(chunk.getChunk().getFileId());
                source.setDocName("文档"); // TODO: 查询文档名称
                source.setChunkText(chunk.getChunk().getChunkText().substring(0, Math.min(100, chunk.getChunk().getChunkText().length())) + "...");
                source.setRelevanceScore(chunk.getScore());
                sources.add(source);
            }
            response.setSources(sources);
        }

        return response;
    }

    @Override
    public void chatStream(ChatRequestDTO request, ChatStreamCallback callback) {
        // TODO: 实现SSE流式响应
        // 目前先用普通模式回调
        try {
            callback.onStart();
            ChatResponseVO response = chat(request);
            callback.onMessage(response.getContent());
            callback.onComplete(response);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    @Override
    public boolean deleteSession(String sessionId) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }

        AiChatSession session = baseMapper.selectBySessionId(sessionId);
        if (session == null) {
            return false;
        }

        if (!session.getUserUuid().equals(loginUser.getUserId().toString())) {
            throw new SecurityException("无权删除该会话");
        }

        session.setStatus(0);
        this.updateById(session);
        return true;
    }

    // ============== 私有方法 ==============

    private AiChatSession createSessionEntity(String kbId, String title) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        AiChatSession session = new AiChatSession();
        session.setSessionId("chat_" + UUID.fastUUID().toString(true));
        session.setUserUuid(loginUser.getUserId().toString());
        session.setKbId(kbId);
        session.setSessionTitle(title + "...");
        session.setSessionType(kbId != null ? 1 : 2);
        session.setModelName(modelName);
        session.setMessageCount(0);
        session.setTotalTokensIn(0);
        session.setTotalTokensOut(0);
        session.setIsPinned(0);
        session.setStatus(1);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        this.save(session);
        return session;
    }

    private AiChatMessage saveUserMessage(String sessionId, String content, String parentMessageId) {
        AiChatMessage message = new AiChatMessage();
        message.setMessageId("msg_" + UUID.fastUUID().toString(true));
        message.setSessionId(sessionId);
        message.setParentMessageId(parentMessageId);
        message.setRole(1); // 用户
        message.setContent(content);
        message.setContentType("text");
        message.setTokensIn(content.length() / 4); // 粗略估算
        message.setTokensOut(0);
        message.setStatus(1);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        return message;
    }

    private AiChatMessage saveAiMessage(String sessionId, String parentMessageId, String content,
                                       int tokensIn, int tokensOut, int latency, String model,
                                       List<IRAGService.ScoredChunk> chunks) {
        AiChatMessage message = new AiChatMessage();
        message.setMessageId("msg_" + UUID.fastUUID().toString(true));
        message.setSessionId(sessionId);
        message.setParentMessageId(parentMessageId);
        message.setRole(2); // AI助手
        message.setContent(content);
        message.setContentType("markdown");
        message.setTokensIn(tokensIn);
        message.setTokensOut(tokensOut);
        message.setLatencyMs(latency);
        message.setModelName(model);

        // 保存检索来源
        if (chunks != null && !chunks.isEmpty()) {
            List<String> chunkIds = chunks.stream()
                    .map(c -> c.getChunk().getChunkId())
                    .collect(Collectors.toList());
            try {
                message.setRetrievalChunks(objectMapper.writeValueAsString(chunkIds));
            } catch (Exception e) {
                log.warn("序列化检索来源失败");
            }
        }

        message.setStatus(1);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        return message;
    }

    private void updateSessionStats(AiChatSession session, LocalDateTime lastMessageAt) {
        session.setMessageCount(session.getMessageCount() + 2); // 用户+AI
        session.setLastMessageAt(lastMessageAt);
        session.setUpdatedAt(LocalDateTime.now());
        this.updateById(session);
    }

    private String buildSystemPrompt(String customPrompt, String context) {
        StringBuilder prompt = new StringBuilder();
        
        if (customPrompt != null && !customPrompt.isEmpty()) {
            prompt.append(customPrompt);
        } else {
            prompt.append("你是一个专业的AI助手，基于提供的参考资料回答问题。");
        }

        if (!context.isEmpty()) {
            prompt.append("\n\n以下是相关参考资料：\n");
            prompt.append(context);
            prompt.append("\n\n请基于以上参考资料回答用户问题。如果资料中没有相关信息，请明确说明。");
        }

        return prompt.toString();
    }

    private LLMResponse callLLM(String systemPrompt, String userContent, Double temp, Integer maxTok) {
        try {
            String url = baseUrl + "/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            List<Map<String, Object>> messages = new ArrayList<>();
            
            Map<String, Object> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);

            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userContent);
            messages.add(userMsg);

            Map<String, Object> body = new HashMap<>();
            body.put("model", modelName);
            body.put("messages", messages);
            body.put("temperature", temp != null ? temp : temperature);
            body.put("max_tokens", maxTok != null ? maxTok : maxTokens);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choice = root.path("choices").get(0);
                String content = choice.path("message").path("content").asText();
                
                int tokensIn = root.path("usage").path("prompt_tokens").asInt();
                int tokensOut = root.path("usage").path("completion_tokens").asInt();

                return new LLMResponse(content, tokensIn, tokensOut);
            } else {
                throw new RuntimeException("LLM API调用失败: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("LLM调用异常: {}", e.getMessage(), e);
            throw new RuntimeException("AI服务暂时不可用: " + e.getMessage());
        }
    }

    private String generateMockResponse(String question, String context) {
        if (!context.isEmpty()) {
            return "根据提供的参考资料，我了解到相关信息。\n\n" +
                   "由于当前处于演示模式（未配置OpenAI API Key），这是模拟回复。\n\n" +
                   "实际部署后，这里将显示基于检索内容的AI生成回答。\n\n" +
                   "检索到的参考片段数：" + context.split("【参考片段").length;
        } else {
            return "你好！这是LingDoc AI助手的模拟回复。\n\n" +
                   "当前处于演示模式，未配置真实的AI模型API Key。\n\n" +
                   "请在application-ai.yml中配置llm.api-key以启用真实对话功能。";
        }
    }

    private ChatSessionVO convertToVO(AiChatSession session) {
        ChatSessionVO vo = new ChatSessionVO();
        BeanUtils.copyProperties(session, vo);
        
        vo.setSessionTypeName(session.getSessionType() == 1 ? "知识库问答" : "自由对话");
        
        if (session.getKbId() != null) {
            var kb = kbMapper.selectByKbId(session.getKbId());
            vo.setKbName(kb != null ? kb.getKbName() : "未知知识库");
        }
        
        return vo;
    }

    private ChatMessageVO convertToMessageVO(AiChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setMessageId(message.getMessageId());
        vo.setRole(message.getRole() == 1 ? "user" : message.getRole() == 2 ? "assistant" : "system");
        vo.setContent(message.getContent());
        vo.setCreatedAt(message.getCreatedAt().toString());
        return vo;
    }

    // 内部类
    private static class LLMResponse {
        private final String content;
        private final int tokensIn;
        private final int tokensOut;

        public LLMResponse(String content, int tokensIn, int tokensOut) {
            this.content = content;
            this.tokensIn = tokensIn;
            this.tokensOut = tokensOut;
        }

        public String getContent() { return content; }
        public int getTokensIn() { return tokensIn; }
        public int getTokensOut() { return tokensOut; }
    }
}
