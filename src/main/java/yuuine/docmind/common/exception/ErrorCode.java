package yuuine.docmind.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    FILE_UPLOAD_FAILED("FILE_UPLOAD_FAILED", "文件上传失败"),
    FILE_NOT_FOUND("FILE_NOT_FOUND", "文件不存在"),
    INVALID_FILE_TYPE("INVALID_FILE_TYPE", "无效的文件类型"),
    DOCUMENT_NOT_FOUND("DOCUMENT_NOT_FOUND", "文档不存在"),
    CHAT_SESSION_NOT_FOUND("CHAT_SESSION_NOT_FOUND", "会话不存在"),
    VECTOR_STORE_ERROR("VECTOR_STORE_ERROR", "向量存储错误"),
    LLM_ERROR("LLM_ERROR", "LLM 调用错误"),
    RERANK_ERROR("RERANK_ERROR", "重排序错误"),
    EMBEDDING_ERROR("EMBEDDING_ERROR", "向量化错误"),
    USER_NOT_FOUND("USER_NOT_FOUND", "用户不存在"),
    USER_ALREADY_EXISTS("USER_ALREADY_EXISTS", "用户已存在"),
    INVALID_PASSWORD("INVALID_PASSWORD", "密码错误"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "无效的凭据"),
    UNAUTHORIZED("UNAUTHORIZED", "未授权"),
    FORBIDDEN("FORBIDDEN", "禁止访问"),
    SESSION_EXPIRED("SESSION_EXPIRED", "会话已过期");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
