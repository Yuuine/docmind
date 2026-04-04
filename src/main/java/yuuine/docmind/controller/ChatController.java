package yuuine.docmind.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import yuuine.docmind.common.model.Result;
import yuuine.docmind.core.audit.annotation.Audited;
import yuuine.docmind.core.audit.valueobject.AuditAction;
import yuuine.docmind.core.chat.dto.ChatMessageRequest;
import yuuine.docmind.core.chat.dto.ChatMessageResponse;
import yuuine.docmind.core.chat.dto.ChatSessionCreateRequest;
import yuuine.docmind.core.chat.dto.ChatSessionResponse;
import yuuine.docmind.core.chat.dto.ChatSessionUpdateRequest;
import yuuine.docmind.core.chat.service.ChatService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Audited(action = AuditAction.CHAT_SESSION_CREATE, resourceType = "ChatSession", resourceIdParam = "userId", describe = "创建聊天会话")
    @PostMapping("/sessions")
    public Result<ChatSessionResponse> createSession(@RequestBody ChatSessionCreateRequest request,
                                                     @RequestParam Long userId) {
        return Result.success(chatService.createSession(request, userId));
    }

    @Audited(action = AuditAction.CHAT_SESSION_UPDATE, resourceType = "ChatSession", resourceIdFromPath = "id", describe = "更新聊天会话")
    @PutMapping("/sessions/{id}")
    public Result<ChatSessionResponse> updateSession(@PathVariable Long id,
                                                      @RequestBody ChatSessionUpdateRequest request,
                                                      @RequestParam Long userId) {
        return Result.success(chatService.updateSession(id, request, userId));
    }

    @GetMapping("/sessions")
    public Result<List<ChatSessionResponse>> listSessions(@RequestParam Long userId) {
        return Result.success(chatService.listSessions(userId));
    }

    @GetMapping("/sessions/{id}/messages")
    public Result<List<ChatMessageResponse>> getMessages(@PathVariable Long id, @RequestParam Long userId) {
        return Result.success(chatService.getSessionMessages(id, userId));
    }

    @Audited(action = AuditAction.CHAT_MESSAGE_SEND, resourceType = "ChatMessage", resourceIdParam = "id", describe = "发送聊天消息")
    @PostMapping("/sessions/{id}/messages")
    public Result<ChatMessageResponse> sendMessage(@PathVariable Long id,
                                                   @RequestBody ChatMessageRequest request,
                                                   @RequestParam Long userId) {
        request.setSessionId(id);
        return Result.success(chatService.sendMessage(request, userId));
    }

    @GetMapping(value = "/sessions/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(@PathVariable Long id,
                                        @RequestParam String content,
                                        @RequestParam Long userId) {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setSessionId(id);
        request.setContent(content);
        return chatService.sendMessageStream(request, userId);
    }

    @Audited(action = AuditAction.CHAT_SESSION_DELETE, resourceType = "ChatSession", resourceIdParam = "id", describe = "删除聊天会话")
    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable Long id, @RequestParam Long userId) {
        chatService.deleteSession(id, userId);
        return Result.success();
    }
}
