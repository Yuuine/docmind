package yuuine.docmind.core.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import yuuine.docmind.core.user.model.User;

@Mapper
public interface UserRepository extends BaseMapper<User> {
}
