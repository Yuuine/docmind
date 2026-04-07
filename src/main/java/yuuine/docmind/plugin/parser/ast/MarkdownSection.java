package yuuine.docmind.plugin.parser.ast;

import lombok.Builder;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Markdown 文档中的一个逻辑章节，对应一个 Heading 节点及其下属内容。
 *
 * <p>本类是 {@link MarkdownDocument} 章节树的节点，具有以下特征：</p>
 * <ul>
 *   <li><b>层级关系</b>：通过 {@code level} 字段记录 Heading 的级别（1-6），
 *       通过 {@code subSections} 支持嵌套子章节（如 H2 下包含 H3）</li>
 *   <li><b>内容聚合</b>：{@code content} 包含该 Heading 下的所有纯文本内容
 *      （段落、列表、代码块、表格等）</li>
 * </ul>
 *
 * <h3>RAG 场景中的价值</h3>
 * <p>在 RAG 分块策略中，每个 {@code MarkdownSection} 可直接作为一个 chunk，
 * 因为它保证了语义完整性——不会将"问题"切在一个 chunk 而"答案"切在下一个 chunk。</p>
 *
 * @see MarkdownDocument
 */
@Data
@Builder
public class MarkdownSection {

    /** 章节标题文本（来自 Heading 节点，不含 # 标记） */
    private String title;

    /** Heading 级别（1=H1, 2=H2, ..., 6=H6） */
    private int level;

    /** 章节的纯文本内容。
     *
     * <p>包含该 Heading 下属的所有内容：段落、列表、代码块、表格、引用等，
     * 已去除 Markdown 格式标记但保留结构分隔符。</p>
     */
    private String content;

    /** 嵌套的子章节列表（level 大于当前章节的 Heading） */
    @Builder.Default
    private List<MarkdownSection> subSections = new ArrayList<>();

    private transient StringBuilder contentBuilder;

    /**
     * 向当前章节追加内容文本（使用 StringBuilder 避免频繁字符串拼接）。
     *
     * @param content 要追加的内容文本
     */
    public void appendContent(String content) {
        if (contentBuilder == null) {
            contentBuilder = new StringBuilder();
        }
        contentBuilder.append(content);
    }

    /**
     * 向当前章节追加一个子章节。
     *
     * @param subSection 要添加的子章节
     */
    public void addSubSection(MarkdownSection subSection) {
        subSections.add(subSection);
    }

    /**
     * 判断当前章节是否包含有效内容。
     *
     * @return {@code true} 如果 content 或 contentBuilder 非空且非空白
     */
    public boolean hasContent() {
        if (content != null && !content.isBlank()) {
            return true;
        }
        return contentBuilder != null && !contentBuilder.isEmpty()
                && contentBuilder.toString().trim().length() > 0;
    }

    /**
     * 获取章节的完整内容文本。
     *
     * <p>优先返回已设置的 content，否则从 contentBuilder 中生成并缓存。</p>
     *
     * @return 章节的纯文本内容（已 trim），若均无则返回 null
     */
    public String getContent() {
        if (content != null) {
            return content;
        }
        if (contentBuilder != null) {
            return contentBuilder.toString().trim();
        }
        return null;
    }
}
