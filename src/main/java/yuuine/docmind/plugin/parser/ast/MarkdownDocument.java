package yuuine.docmind.plugin.parser.ast;

import lombok.Builder;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Markdown 文档的完整结构化模型（AST 高层封装）。
 *
 * <p>本类是 {@link yuuine.docmind.plugin.parser.visitor.RagMarkdownVisitor} 遍历 CommonMark AST 后
 * 产出的聚合结果，封装了文档的多维度视图：</p>
 *
 * <h3>数据字段</h3>
 * <table border="1">
 *   <tr><th>字段</th><th>类型</th><th>说明</th></tr>
 *   <tr><td>{@code rawText}</td><td>String</td><td>原始 Markdown 文本（未做任何处理）</td></tr>
 *   <tr><td>{@code plainText}</td><td>String</td><td>纯文本内容（去除格式标记，保留结构分隔符）</td></tr>
 *   <tr><td>{@code sections}</td><td>List&lt;MarkdownSection&gt;</td><td>按 Heading 组织的章节树（支持嵌套）</td></tr>
 *   <tr><td>{@code tables}</td><td>List&lt;MarkdownTableData&gt;</td><td>所有 GFM 表格的结构化数据</td></tr>
 *   <tr><td>{@code headings}</td><td>List&lt;String&gt;</td><td>所有标题文本的扁平列表</td></tr>
 *   <tr><td>{@code codeBlockCount}</td><td>int</td><td>代码块数量统计</td></tr>
 * </table>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>纯文本提取</b>：{@code getPlainText()} 用于向量化、语义检索</li>
 *   <li><b>智能切片</b>：{@code getSections()} 按 Heading 边界切分，保持语义完整性</li>
 *   <li><b>结构化问答</b>：{@code getTables()} 提供表格 Key-Value 映射，支持精确匹配</li>
 *   <li><b>质量评估</b>：{@code getCodeBlockCount()}、{@code getHeadings().size()} 等元数据用于文档质量分析</li>
 * </ul>
 *
 * @see MarkdownSection
 * @see MarkdownTableData
 * @see yuuine.docmind.plugin.parser.visitor.RagMarkdownVisitor
 */
@Data
@Builder
public class MarkdownDocument {

    /** 原始 Markdown 文本（未做任何处理） */
    private String rawText;

    /** 纯文本内容（去除所有格式标记，但保留段落、列表等结构分隔符） */
    private String plainText;

    /** 按 Heading 层级组织的章节树，顶层章节对应 H1/H2 标题 */
    @Builder.Default
    private List<MarkdownSection> sections = new ArrayList<>();

    /** 文档中所有 GFM 表格的结构化数据 */
    @Builder.Default
    private List<MarkdownTableData> tables = new ArrayList<>();

    /** 所有标题文本的扁平列表（按出现顺序） */
    @Builder.Default
    private List<String> headings = new ArrayList<>();

    /** 文档中代码块的总数（围栏 + 缩进） */
    @Builder.Default
    private int codeBlockCount = 0;

    /**
     * 向文档添加一个顶级章节。
     *
     * @param section 要添加的章节节点
     */
    public void addSection(MarkdownSection section) {
        sections.add(section);
    }

    /**
     * 向文档添加一个表格数据。
     *
     * @param table 要添加的表格
     */
    public void addTable(MarkdownTableData table) {
        tables.add(table);
    }

    /**
     * 向标题列表中追加一个标题文本。
     *
     * @param heading 标题文本
     */
    public void addHeading(String heading) {
        headings.add(heading);
    }

    /** 代码块计数器自增 */
    public void incrementCodeBlockCount() {
        codeBlockCount++;
    }
}
