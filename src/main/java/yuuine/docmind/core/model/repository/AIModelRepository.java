package yuuine.docmind.core.model.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import yuuine.docmind.core.model.entity.AIModel;

@Mapper
public interface AIModelRepository extends BaseMapper<AIModel> {
}
