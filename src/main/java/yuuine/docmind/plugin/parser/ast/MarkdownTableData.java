package yuuine.docmind.plugin.parser.ast;

import lombok.Builder;
import lombok.Data;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GFM（GitHub Flavored Markdown）表格的结构化数据模型。
 *
 * <p>本类将 Markdown 表格的二维文本转换为具有语义的结构化数据，
 * 核心特性是保留了<strong>列名与单元格值的映射关系</strong>（Key-Value）。</p>
 *
 * <h3>数据结构</h3>
 * <pre>
 * 表头: ["参数名", "类型", "默认值"]
 * 行数据: [
 *   {"参数名":"port", "类型":"int", "默认值":"8080"},
 *   {"参数名":"host", "类型":"string", "默认值":"localhost"}
 * ]
 * </pre>
 *
 * <h3>RAG 场景中的价值</h3>
 * <ul>
 *   <li><b>精确匹配</b>：用户查询"端口默认值是多少？"可精确定位到
 *       {@code getCellValue(0, "默认值")} = "8080"</li>
 *   <li><b>上下文增强</b>：检索结果可附带表格元数据（表头、行列位置），
 *       提升回答准确性</li>
 *   <li><b>避免歧义</b>：相比纯文本中 "8080 port int" 的扁平输出，
 *       结构化数据明确标识 8080 是"默认值"列而非"参数名"列</li>
 * </ul>
 *
 * @see MarkdownDocument
 */
@Data
@Builder
public class MarkdownTableData {

    /** 表头列名列表（按原始顺序） */
    private List<String> headers;

    /**
     * 行数据列表。
     *
     * <p>每行是一个 {@code Map&lt;String, String&gt;}，key 为列名（来自 headers），
     * value 为该单元格的纯文本内容。使用 {@link LinkedHashMap} 保证插入顺序。</p>
     */
    @Builder.Default
    private List<Map<String, String>> rows = new ArrayList<>();

    /** 原始 Markdown 表格文本（保留用于回显或调试） */
    private String rawMarkdown;

    /**
     * 按行列名获取指定单元格的值。
     *
     * @param rowIndex   行索引（0-based）
     * @param columnName 列名（需与 headers 中的名称匹配）
     * @return 单元格值；若索引越界或列名不存在则返回 {@code null}
     */
    public String getCellValue(int rowIndex, String columnName) {
        if (rowIndex >= 0 && rowIndex < rows.size()) {
            return rows.get(rowIndex).get(columnName);
        }
        return null;
    }

    /**
     * 向表格追加一行数据。
     *
     * @param row 行数据映射（key=列名, value=单元格值）
     */
    public void addRow(Map<String, String> row) {
        rows.add(row);
    }
}
