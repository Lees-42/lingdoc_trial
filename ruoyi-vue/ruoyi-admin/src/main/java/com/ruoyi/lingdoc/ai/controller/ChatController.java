package com.ruoyi.lingdoc.ai.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.lingdoc.ai.domain.dto.ChatRequestDTO;
import com.ruoyi.lingdoc.ai.domain.vo.ChatResponseVO;
import com.ruoyi.lingdoc.ai.domain.vo.ChatSessionVO;
import com.ruoyi.lingdoc.ai.service.IChatService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI对话Controller
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/ai/chat")
public class ChatController extends BaseController {

    @Autowired
    private IChatService chatService;

    /**
     * 获取会话列表
     */
    @GetMapping("/sessions")
    public TableDataInfo listSessions() {
        startPage();
        List<ChatSessionVO> list = chatService.listSessions();
        return getDataTable(list);
    }

    /**
     * 创建新会话
     */
    @PostMapping("/sessions")
    public AjaxResult createSession(@RequestParam(value = "kbId", required = false) String kbId,
                                    @RequestParam(value = "title", defaultValue = "新对话") String title) {
        log.info("创建会话: kbId={}, title={}", kbId, title);
        ChatSessionVO session = chatService.createSession(kbId, title);
        return success(session);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public AjaxResult deleteSession(@PathVariable String sessionId) {
        log.info("删除会话: sessionId={}", sessionId);
        boolean result = chatService.deleteSession(sessionId);
        return result ? success() : error("会话不存在或无权删除");
    }

    /**
     * 获取会话消息历史
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public AjaxResult getMessages(@PathVariable String sessionId) {
        List<IChatService.ChatMessageVO> messages = chatService.getMessages(sessionId);
        return success(messages);
    }

    /**
     * 发送消息（普通模式）
     */
    @PostMapping("/send")
    public AjaxResult chat(@Valid @RequestBody ChatRequestDTO request) {
        log.info("发送消息: sessionId={}, kbId={}", request.getSessionId(), request.getKbId());
        ChatResponseVO response = chatService.chat(request);
        return success(response);
    }
}
