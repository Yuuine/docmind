package yuuine.docmind.core.document.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DocumentBatchDeleteRequest {

    @NotEmpty(message = "文档 ID 列表不能为空")
    @Size(max = 100, message = "单次最多删除 100 个文档")
    private List<Long> ids;
}
