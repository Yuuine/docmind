package yuuine.docmind.core.document.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import yuuine.docmind.core.document.model.DocumentChunk;

@Mapper
public interface DocumentChunkRepository extends BaseMapper<DocumentChunk> {
}
