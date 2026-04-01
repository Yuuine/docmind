package yuuine.docmind.controller;

import org.springframework.web.bind.annotation.*;
import yuuine.docmind.common.model.Result;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @PostMapping("/register")
    public Result<Object> register() {
        return Result.success(null);
    }

    @PostMapping("/login")
    public Result<Object> login() {
        return Result.success(null);
    }

    @GetMapping("/profile")
    public Result<Object> getProfile() {
        return Result.success(null);
    }

    @PutMapping("/profile")
    public Result<Object> updateProfile() {
        return Result.success(null);
    }
}
