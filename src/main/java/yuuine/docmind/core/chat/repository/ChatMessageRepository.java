package yuuine.docmind.core.chat.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import yuuine.docmind.core.chat.model.ChatMessage;

@Mapper
public interface ChatMessageRepository extends BaseMapper<ChatMessage> {
}
