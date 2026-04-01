package yuuine.docmind.controller;

import org.springframework.web.bind.annotation.*;
import yuuine.docmind.common.model.Result;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    @GetMapping("/logs")
    public Result<Object> getLogs() {
        return Result.success(null);
    }
}
