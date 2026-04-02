package yuuine.docmind.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import yuuine.docmind.common.model.Result;
import yuuine.docmind.core.audit.annotation.Audited;
import yuuine.docmind.core.audit.valueobject.AuditAction;
import yuuine.docmind.core.user.dto.UserLoginRequest;
import yuuine.docmind.core.user.dto.UserRegisterRequest;
import yuuine.docmind.core.user.dto.UserResponse;
import yuuine.docmind.core.user.dto.UserUpdateRequest;
import yuuine.docmind.core.user.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Audited(
            action = AuditAction.USER_REGISTER,
            resourceType = "User",
            describe = "用户注册",
            sensitiveParams = {"password"}
    )
    @PostMapping("/register")
    public Result<UserResponse> register(@RequestBody UserRegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @Audited(
            action = AuditAction.USER_LOGIN,
            resourceType = "User",
            resourceIdParam = "request",
            describe = "用户登录",
            sensitiveParams = {"password"}
    )
    @PostMapping("/login")
    public Result<UserResponse> login(@Valid @RequestBody UserLoginRequest request) {
        return Result.success(userService.login(request));
    }

    @GetMapping("/profile")
    public Result<UserResponse> getProfile(@RequestParam Long userId) {
        return Result.success(userService.getProfile(userId));
    }

    @Audited(
            action = AuditAction.USER_UPDATE_PROFILE,
            resourceType = "User",
            resourceIdParam = "userId",
            describe = "更新用户资料",
            sensitiveParams = {"oldPassword", "newPassword"}
    )
    @PutMapping("/profile")
    public Result<UserResponse> updateProfile(@RequestParam Long userId, @RequestBody UserUpdateRequest request) {
        return Result.success(userService.updateProfile(userId, request));
    }

    @Audited(
            action = AuditAction.DELETE_USER,
            resourceType = "User",
            resourceIdFromPath = "userId",
            describe = "删除用户",
            logRequest = false,
            logResponse = false
    )
    @DeleteMapping("/{userId}")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return Result.success();
    }
}
