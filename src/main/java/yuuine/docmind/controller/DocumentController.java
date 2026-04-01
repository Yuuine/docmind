package yuuine.docmind.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yuuine.docmind.common.model.Result;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    @PostMapping("/upload")
    public Result<Object> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(null);
    }

    @GetMapping
    public Result<Object> list() {
        return Result.success(null);
    }

    @GetMapping("/{id}")
    public Result<Object> getDetail(@PathVariable Long id) {
        return Result.success(null);
    }

    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id) {
    }

    @DeleteMapping("/{id}")
    public Result<Object> delete(@PathVariable Long id) {
        return Result.success(null);
    }
}
