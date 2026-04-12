package yuuine.docmind.plugin.parser.visitor;

import lombok.extern.slf4j.Slf4j;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.node.*;
import org.commonmark.renderer.text.TextContentRenderer;
import yuuine.docmind.plugin.parser.ast.MarkdownDocument;
import yuuine.docmind.plugin.parser.ast.MarkdownSection;
import yuuine.docmind.plugin.parser.ast.MarkdownTableData;

import java.util.*;

/**
 * CommonMark AST 遍历器，用于构建 RAG 场景下的结构化文档模型。
 *
 * <p>本类是 {@link org.commonmark.node.AbstractVisitor} 的实现，采用访问者设计模式遍历
 * CommonMark 解析器生成的抽象语法树（AST），将 Markdown 文档转换为 {@link MarkdownDocument}
 * 结构化模型。该模型包含多维度的文档信息，适用于 RAG 系统的向量化、智能切片和结构化问答。</p>
 *
 * <h3>核心功能</h3>
 * <ul>
 *   <li><b>章节树构建</b>：基于 Heading 节点自动构建章节层级关系（支持 H1-H6 嵌套）</li>
 *   <li><b>纯文本提取</b>：去除 Markdown 格式标记，但保留段落、列表等结构分隔符</li>
 *   <li><b>表格结构化</b>：提取 GFM 表格的表头和行数据，构建 Key-Value 映射</li>
 *   <li><b>元数据统计</b>：代码块数量、标题列表等文档元数据收集</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * Parser parser = Parser.builder().extensions(List.of(TablesExtension.create())).build();
 * Node astRoot = parser.parse(markdownContent);
 *
 * RagMarkdownVisitor visitor = new RagMarkdownVisitor();
 * astRoot.accept(visitor);
 *
 * MarkdownDocument document = visitor.getDocument();
 * }</pre>
 *
 * <h3>章节栈机制</h3>
 * <p>使用 {@link Deque} 管理章节嵌套关系，遇到新 Heading 时弹出栈中 level &gt;= 当前级别的
 * 章节，确保层级正确性。例如：H1 → H2 → H3 → H2 时，H3 和第二个 H2 前的 H2 会被正确关闭。</p>
 *
 * <h3>线程安全性</h3>
 * <p>本类<strong>非线程安全</strong>，每个实例应仅用于单次 AST 遍历。
 * {@link #TEXT_RENDERER} 是线程安全的静态实例，可在多个访问者实例间共享。</p>
 *
 * @see org.commonmark.node.AbstractVisitor
 * @see MarkdownDocument
 * @see MarkdownSection
 * @see MarkdownTableData
 */
@Slf4j
public class RagMarkdownVisitor extends AbstractVisitor {

    /** 线程安全的文本内容渲染器实例（缓存复用，避免每次 extractText 都创建新实例） */
    private static final TextContentRenderer TEXT_RENDERER = TextContentRenderer.builder().build();

    /** 聚合输出的文档模型，遍历完成后通过 {@link #getDocument()} 获取 */
    private final MarkdownDocument document;

    /** 纯文本构建器，逐步拼接所有节点的文本输出 */
    private final StringBuilder plainTextBuilder = new StringBuilder();

    /**
     * Heading 层级栈，用于管理章节嵌套关系。
     *
     * <p>当遇到新 Heading 时：弹出栈中 level &gt;= 当前级别的章节（完成关闭），
     * 然后将当前章节压栈。保证 H1 > H2 > H3 的层级正确性。</p>
     */
    private final Deque<MarkdownSection> sectionStack = new ArrayDeque<>();

    /** 当前活跃的章节节点（栈顶），新内容追加到此章节 */
    private MarkdownSection currentSection;

    /** 创建一个新的 RAG Markdown 访问器实例 */
    public RagMarkdownVisitor() {
        this.document = MarkdownDocument.builder().build();
        this.currentSection = null;
    }

    /**
     * 获取遍历完成后的完整文档模型。
     *
     * <p>必须在 AST 遍历完成后（即 {@code node.accept(this)} 返回后）调用，
     * 此时 plainText 已构建完毕并设置到 document 中。</p>
     *
     * @return 包含纯文本、章节树、表格数据的完整文档模型
     */
    public MarkdownDocument getDocument() {
        while (!sectionStack.isEmpty()) {
            MarkdownSection remaining = sectionStack.pop();
            if (remaining.hasContent()) {
                if (sectionStack.isEmpty()) {
                    document.addSection(remaining);
                } else {
                    sectionStack.peek().addSubSection(remaining);
                }
            }
        }
        document.setPlainText(plainTextBuilder.toString().trim());
        return document;
    }

    /**
     * 访问文档根节点——启动整个 AST 遍历过程。
     *
     * <p>这是访问者模式的入口点，递归遍历文档的所有子节点。</p>
     *
     * @param documentNode Markdown 文档的根 AST 节点
     */
    @Override
    public void visit(Document documentNode) {
        visitChildren(documentNode);
    }

    /**
     * 访问 Heading 节点——章节树构建的核心逻辑。
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>提取标题文本和级别</li>
     *   <li>将标题追加到纯文本输出和文档 headings 列表</li>
     *   <li><b>栈操作</b>：弹出栈中 level &gt;= 当前级别的章节（这些章节已结束），
     *       将完成的章节添加到文档或父章节的 subSections 中</li>
     *   <li>如果当前章节有内容且未被处理，先保存它</li>
     *   <li>创建新章节并压栈，设为 currentSection</li>
     * </ol>
     *
     * @param heading 标题节点（包含级别和文本子节点）
     */
    @Override
    public void visit(Heading heading) {
        String titleText = extractText(heading);
        int level = heading.getLevel();

        document.addHeading(titleText);

        plainTextBuilder.append("\n").append(titleText).append("\n\n");

        MarkdownSection newSection = MarkdownSection.builder()
                .title(titleText)
                .level(level)
                .build();

        while (!sectionStack.isEmpty() && sectionStack.peek().getLevel() >= level) {
            MarkdownSection completed = sectionStack.pop();
            if (completed.hasContent()) {
                if (sectionStack.isEmpty()) {
                    document.addSection(completed);
                } else {
                    sectionStack.peek().addSubSection(completed);
                }
            }
        }

        sectionStack.push(newSection);
        currentSection = newSection;
        appendToCurrentSection(titleText + "\n\n");
    }

    /**
     * 访问段落节点——提取纯文本内容并追加到当前章节。
     *
     * @param paragraph 段落 AST 节点
     */
    @Override
    public void visit(Paragraph paragraph) {
        String text = extractText(paragraph);
        appendToCurrentSection(text);
        plainTextBuilder.append(text).append("\n\n");
    }

    /**
     * 访问无序列表节点——添加换行分隔符并遍历列表项。
     *
     * @param bulletList 无序列表 AST 节点
     */
    @Override
    public void visit(BulletList bulletList) {
        plainTextBuilder.append("\n");
        visitChildren(bulletList);
    }

    /**
     * 访问有序列表节点——添加换行分隔符并遍历列表项。
     *
     * @param orderedList 有序列表 AST 节点
     */
    @Override
    public void visit(OrderedList orderedList) {
        plainTextBuilder.append("\n");
        visitChildren(orderedList);
    }

    /**
     * 访问列表项节点——生成带缩进前缀的列表项文本。
     *
     * <p>根据嵌套层级计算缩进，使用 "* " 作为统一的列表标记（有序/无序列表统一处理）。</p>
     *
     * @param listItem 列表项 AST 节点
     */
    @Override
    public void visit(ListItem listItem) {
        int indent = getListItemIndent(listItem);
        String prefix = "  ".repeat(Math.max(0, indent - 1)) + "* ";

        plainTextBuilder.append(prefix);
        appendToCurrentSection(prefix);

        visitChildren(listItem);

        plainTextBuilder.append("\n");
        appendToCurrentSection("\n");
    }

    /**
     * 访问围栏代码块节点——保留代码块格式（含语言标识）。
     *
     * <p>保留原始的 ``` 围栏标记和语言信息，确保代码块在纯文本输出中仍可识别。</p>
     *
     * @param codeBlock 围栏代码块 AST 节点
     */
    @Override
    public void visit(FencedCodeBlock codeBlock) {
        String literal = codeBlock.getLiteral();
        String info = codeBlock.getInfo();

        StringBuilder codeOutput = new StringBuilder();
        codeOutput.append("```");
        if (info != null && !info.isBlank()) {
            codeOutput.append(info);
        }
        codeOutput.append("\n").append(literal).append("```\n\n");

        String content = codeOutput.toString();
        appendToCurrentSection(content);
        plainTextBuilder.append(content);

        document.incrementCodeBlockCount();
    }

    /**
     * 访问缩进代码块节点——保留代码的缩进格式。
     *
     * @param codeBlock 缩进代码块 AST 节点
     */
    @Override
    public void visit(IndentedCodeBlock codeBlock) {
        String literal = codeBlock.getLiteral();
        String content = literal + "\n\n";

        appendToCurrentSection(content);
        plainTextBuilder.append(content);

        document.incrementCodeBlockCount();
    }

    /**
     * 访问分隔线节点（---、***、___）。
     *
     * @param thematicBreak 分隔线节点
     */
    @Override
    public void visit(ThematicBreak thematicBreak) {
        String content = "---\n\n";
        plainTextBuilder.append(content);
        appendToCurrentSection(content);
    }

    /**
     * 访问块引用节点——添加 ">" 前缀保留引用格式。
     *
     * <p>多行引用会在每一行前添加 "> " 前缀，保持块引用的视觉结构。</p>
     *
     * @param blockQuote 块引用 AST 节点
     */
    @Override
    public void visit(BlockQuote blockQuote) {
        String text = extractText(blockQuote);
        String quoted = "> " + text.replace("\n", "\n> ") + "\n\n";

        appendToCurrentSection(quoted);
        plainTextBuilder.append(quoted);
    }
    
    /**
     * 访问自定义块节点——处理扩展的节点类型（如 GFM 表格）。
     *
     * <p>CommonMark 扩展节点（如 TableBlock）通过此方法处理，需要在方法内部
     * 检查具体的节点类型并进行相应处理。</p>
     *
     * @param customBlock 自定义块 AST 节点
     */
    @Override
    public void visit(CustomBlock customBlock) {
        if (customBlock instanceof TableBlock tableBlock) {
            visitTableBlock(tableBlock);
        } else {
            super.visit(customBlock);
        }
    }

    /**
     * 访问 GFM 表格节点——提取表头和行数据，构建结构化表格模型。
     *
     * <p>解析流程：
     * <ol>
     *   <li>从 TableHead 提取表头文本</li>
     *   <li>从 TableBody 逐行提取单元格数据，使用 LinkedHashMap 保持列顺序</li>
     *   <li>构建 Markdown 格式的表格字符串用于纯文本输出</li>
     *   <li>创建 {@link MarkdownTableData} 对象添加到文档中</li>
     * </ol>
     *
     * @param tableBlock GFM 表格 AST 节点
     */
    private void visitTableBlock(TableBlock tableBlock) {
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> rows = new ArrayList<>();

        Node child = tableBlock.getFirstChild();

        while (child != null) {
            if (child instanceof TableHead tableHead) {
                Node headerRow = tableHead.getFirstChild();
                while (headerRow != null) {
                    if (headerRow instanceof TableRow) {
                        Node cell = headerRow.getFirstChild();
                        while (cell != null) {
                            if (cell instanceof TableCell) {
                                headers.add(extractTextFromNode(cell));
                            }
                            cell = cell.getNext();
                        }
                    }
                    headerRow = headerRow.getNext();
                }
            } else if (child instanceof TableBody tableBody) {
                Node rowNode = tableBody.getFirstChild();
                while (rowNode != null) {
                    if (rowNode instanceof TableRow) {
                        Map<String, String> rowData = new LinkedHashMap<>();
                        int cellIndex = 0;
                        Node cellNode = rowNode.getFirstChild();
                        while (cellNode != null) {
                            if (cellNode instanceof TableCell) {
                                String headerName = cellIndex < headers.size() ? headers.get(cellIndex) : "col" + cellIndex;
                                rowData.put(headerName, extractTextFromNode(cellNode));
                                cellIndex++;
                            }
                            cellNode = cellNode.getNext();
                        }
                        rows.add(rowData);
                    }
                    rowNode = rowNode.getNext();
                }
            }
            child = child.getNext();
        }

        StringBuilder tableOutput = new StringBuilder();
        tableOutput.append(String.join(" | ", headers)).append("\n");
        tableOutput.append("---|".repeat(headers.size())).append("\n");

        for (Map<String, String> rowData : rows) {
            List<String> values = new ArrayList<>();
            for (String header : headers) {
                values.add(rowData.getOrDefault(header, ""));
            }
            tableOutput.append(String.join(" | ", values)).append("\n");
        }
        tableOutput.append("\n");

        MarkdownTableData tableData = MarkdownTableData.builder()
                .headers(headers)
                .rows(rows)
                .rawMarkdown(tableOutput.toString())
                .build();

        document.addTable(tableData);

        String tableStr = tableOutput.toString();
        appendToCurrentSection(tableStr);
        plainTextBuilder.append(tableStr);
    }

    /**
     * 直接从节点及其子节点中提取文本内容（不依赖访问者机制）。
     *
     * <p>此方法用于手动提取自定义节点（如 TableCell）的文本内容，
     * 因为这些自定义节点的子节点不会被访问者自动遍历。</p>
     *
     * @param node 要提取文本的节点
     * @return 节点及其子节点的纯文本内容
     */
    private String extractTextFromNode(Node node) {
        StringBuilder sb = new StringBuilder();
        extractTextFromNodeRecursive(node, sb);
        return sb.toString().trim();
    }

    /**
     * 递归提取节点及其子节点的文本内容。
     *
     * @param node 当前节点
     * @param sb 用于拼接文本的 StringBuilder
     */
    private void extractTextFromNodeRecursive(Node node, StringBuilder sb) {
        if (node instanceof Text text) {
            sb.append(text.getLiteral());
        }

        Node child = node.getFirstChild();
        while (child != null) {
            extractTextFromNodeRecursive(child, sb);
            child = child.getNext();
        }
    }

    /**
     * 访问链接节点——提取链接文本和目标 URL。
     *
     * <p>输出格式为 "文本 (URL)"，确保链接信息在纯文本中不丢失。</p>
     *
     * @param link 链接 AST 节点
     */
    @Override
    public void visit(Link link) {
        String text = extractText(link);
        String destination = link.getDestination();

        String content;
        if (destination != null && !destination.isEmpty()) {
            content = text + " (" + destination + ")";
        } else {
            content = text;
        }

        plainTextBuilder.append(content);
        appendToCurrentSection(content);
    }

    /**
     * 访问图片节点——提取 alt 文本并标记为图片。
     *
     * <p>使用 {@link #extractText(Node)} 而非 {@code getTitle()} 提取 alt 文本，
     * 因为 CommonMark 的 {@code getTitle()} 返回的是 title 属性（可选），
     * 而 alt 文本存储在子节点中。</p>
     *
     * @param image 图片节点
     */
    @Override
    public void visit(Image image) {
        String altText = extractText(image);
        if (altText == null || altText.isBlank()) {
            altText = image.getTitle() != null ? image.getTitle() : "";
        }
        String content = "[图片: " + altText + "]";

        plainTextBuilder.append(content);
        appendToCurrentSection(content);
    }

    /**
     * 访问斜体强调节点——提取纯文本（去除 * 标记）。
     *
     * <p>注意：仅调用 {@link #extractText(Node)} 即可，TextContentRenderer 会递归处理子节点。
     * 不再调用 {@code visitChildren()}，否则子节点文本会被重复输出。</p>
     *
     * @param emphasis 斜体节点
     */
    @Override
    public void visit(Emphasis emphasis) {
        plainTextBuilder.append(extractText(emphasis));
        appendToCurrentSection(extractText(emphasis));
    }

    /**
     * 访问粗体强调节点——提取纯文本（去除 ** 标记）。
     *
     * @param strongEmphasis 粗体节点
     */
    @Override
    public void visit(StrongEmphasis strongEmphasis) {
        plainTextBuilder.append(extractText(strongEmphasis));
        appendToCurrentSection(extractText(strongEmphasis));
    }

    /**
     * 访问行内代码节点（{@code `code`}）——提取代码内容。
     *
     * @param code 行内代码节点
     */
    @Override
    public void visit(Code code) {
        plainTextBuilder.append(code.getLiteral());
    }

    /**
     * 访问文本节点——直接追加文本内容。
     *
     * @param text 纯文本节点
     */
    @Override
    public void visit(Text text) {
        plainTextBuilder.append(text.getLiteral());
    }

    /**
     * 访问软换行节点（Markdown 中的普通换行）——转换为空格。
     *
     * @param softLineBreak 软换行节点
     */
    @Override
    public void visit(SoftLineBreak softLineBreak) {
        plainTextBuilder.append(" ");
    }

    /**
     * 访问硬换行节点（Markdown 中的两个空格+换行或反斜杠+换行）——转换为换行符。
     *
     * @param hardLineBreak 硬换行节点
     */
    @Override
    public void visit(HardLineBreak hardLineBreak) {
        plainTextBuilder.append("\n");
    }

    /**
     * 递归遍历父节点的所有子节点。
     *
     * <p>这是访问者模式的核心遍历方法，会对每个子节点调用 {@code accept(this)}，
     * 实现 AST 的深度优先遍历。重写此方法是为了明确标注其作用。</p>
     *
     * @param parent 要遍历子节点的父 AST 节点
     */
    @Override
    protected void visitChildren(Node parent) {
        Node node = parent.getFirstChild();
        while (node != null) {
            Node next = node.getNext();
            node.accept(this);
            node = next;
        }
    }

    /**
     * 使用 CommonMark 的 {@link TextContentRenderer} 提取节点的纯文本内容。
     *
     * <p>使用缓存的 {@link #TEXT_RENDERER} 实例（线程安全），
     * 自动递归处理子节点，去除所有格式标记，返回干净的纯文本。</p>
     *
     * @param node 要提取文本的 AST 节点
     * @return 节点的纯文本内容（已 trim）
     */
    private String extractText(Node node) {
        return TEXT_RENDERER.render(node).trim();
    }

    /**
     * 将内容文本追加到当前活跃章节的 content 字段。
     *
     * <p>如果 currentSection 为 null（文档开头无 Heading 的内容），则静默丢弃。
     * 已有内容时采用追加模式（非覆盖）。</p>
     *
     * @param content 要追加的内容文本
     */
    private void appendToCurrentSection(String content) {
        if (currentSection != null) {
            currentSection.appendContent(content);
        }
    }

    /**
     * 计算列表项的嵌套缩进层级。
     *
     * <p>通过向上遍历父节点链（ListItem → BulletList/OrderedList → ...），
     * 统计嵌套深度。返回值范围 [1, 6]，用于生成缩进的列表前缀。</p>
     *
     * @param item 列表项节点
     * @return 缩进层级（1=顶层，最大 6）
     */
    private int getListItemIndent(ListItem item) {
        int indent = 1;
        Node parent = item.getParent();
        while (parent instanceof ListItem || parent instanceof BulletList || parent instanceof OrderedList) {
            indent++;
            parent = parent.getParent();
        }
        return Math.min(indent, 6);
    }
}
