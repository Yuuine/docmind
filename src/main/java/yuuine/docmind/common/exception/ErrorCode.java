package yuuine.docmind.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    FILE_UPLOAD_FAILED(400, "FILE_UPLOAD_FAILED", "文件上传失败"),
    FILE_NOT_FOUND(404, "FILE_NOT_FOUND", "文件不存在"),
    INVALID_FILE_TYPE(400, "INVALID_FILE_TYPE", "无效的文件类型"),
    DOCUMENT_NOT_FOUND(404, "DOCUMENT_NOT_FOUND", "文档不存在"),
    CHAT_SESSION_NOT_FOUND(404, "CHAT_SESSION_NOT_FOUND", "会话不存在"),
    VECTOR_STORE_ERROR(500, "VECTOR_STORE_ERROR", "向量存储错误"),
    LLM_ERROR(500, "LLM_ERROR", "LLM 调用错误"),
    RERANK_ERROR(500, "RERANK_ERROR", "重排序错误"),
    EMBEDDING_ERROR(500, "EMBEDDING_ERROR", "向量化错误"),
    USER_NOT_FOUND(404, "USER_NOT_FOUND", "用户不存在"),
    USER_ALREADY_EXISTS(409, "USER_ALREADY_EXISTS", "用户已存在"),
    INVALID_PASSWORD(401, "INVALID_PASSWORD", "密码错误"),
    INVALID_CREDENTIALS(401, "INVALID_CREDENTIALS", "用户名或密码错误"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "未授权"),
    FORBIDDEN(403, "FORBIDDEN", "禁止访问"),
    SESSION_EXPIRED(401, "SESSION_EXPIRED", "会话已过期");

    private final int httpCode;
    private final String code;
    private final String message;

    ErrorCode(int httpCode, String code, String message) {
        this.httpCode = httpCode;
        this.code = code;
        this.message = message;
    }
}
