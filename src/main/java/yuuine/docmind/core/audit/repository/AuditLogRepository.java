package yuuine.docmind.core.audit.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import yuuine.docmind.core.audit.model.AuditLog;

@Mapper
public interface AuditLogRepository extends BaseMapper<AuditLog> {
}
