package yuuine.docmind.controller;

import org.springframework.web.bind.annotation.*;
import yuuine.docmind.common.model.Result;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    @PostMapping("/sessions")
    public Result<Object> createSession() {
        return Result.success(null);
    }

    @GetMapping("/sessions")
    public Result<Object> listSessions() {
        return Result.success(null);
    }

    @GetMapping("/sessions/{id}/messages")
    public Result<Object> getMessages(@PathVariable Long id) {
        return Result.success(null);
    }

    @PostMapping("/sessions/{id}/messages")
    public void sendMessage(@PathVariable Long id) {
    }

    @DeleteMapping("/sessions/{id}")
    public Result<Object> deleteSession(@PathVariable Long id) {
        return Result.success(null);
    }
}
