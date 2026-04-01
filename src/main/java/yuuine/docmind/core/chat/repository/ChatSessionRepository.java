package yuuine.docmind.core.chat.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import yuuine.docmind.core.chat.model.ChatSession;

@Mapper
public interface ChatSessionRepository extends BaseMapper<ChatSession> {
}
