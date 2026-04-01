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
    EMBEDDING_ERROR("EMBEDDING_ERROR", "向量化错误");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
