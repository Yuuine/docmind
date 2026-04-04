package yuuine.docmind.core.chat.valueobject;

import lombok.Data;

@Data
public class LlmChunk {

    private String content;
    private boolean done;
    private String error;

    public static LlmChunk of(String content, boolean done) {
        LlmChunk chunk = new LlmChunk();
        chunk.setContent(content);
        chunk.setDone(done);
        chunk.setError(null);
        return chunk;
    }

    public static LlmChunk error(String errorMessage) {
        LlmChunk chunk = new LlmChunk();
        chunk.setContent(null);
        chunk.setDone(true);
        chunk.setError(errorMessage);
        return chunk;
    }
}
