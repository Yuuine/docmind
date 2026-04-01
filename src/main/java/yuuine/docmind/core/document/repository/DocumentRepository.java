package yuuine.docmind.core.document.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import yuuine.docmind.core.document.model.Document;

@Mapper
public interface DocumentRepository extends BaseMapper<Document> {
}
