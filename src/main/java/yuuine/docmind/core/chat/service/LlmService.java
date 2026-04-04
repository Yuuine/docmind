package yuuine.docmind.core.chat.service;

import reactor.core.publisher.Flux;
import yuuine.docmind.core.chat.valueobject.LlmChunk;
import yuuine.docmind.core.chat.model.ChatMessage;
import yuuine.docmind.core.model.entity.AIModel;

import java.util.List;

public interface LlmService {

    Flux<LlmChunk> streamChat(AIModel model, List<ChatMessage> messages);

}
