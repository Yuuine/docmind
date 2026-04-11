package yuuine.docmind.plugin.parser;

import lombok.extern.slf4j.Slf4j;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import yuuine.docmind.common.plugin.ParserPlugin;
import yuuine.docmind.plugin.parser.ast.MarkdownDocument;
import yuuine.docmind.plugin.parser.ast.MarkdownSection;
import yuuine.docmind.plugin.parser.visitor.RagMarkdownVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 CommonMark AST 的企业级 Markdown 文档解析器。
 *
 * <p>本解析器采用编译原理中的标准流程实现：词法分析 → 语法分析 → AST 构建 → 遍历渲染，
 * 相比传统的正则替换方案，具有以下核心优势：</p>
 *
 * <ul>
 *   <li><b>完整语法理解</b>：正确处理代码块（含空行）、嵌套列表、GFM 表格等跨行结构</li>
 *   <li><b>结构化切片</b>：按 Heading 节点自动切分章节，保持语义单元完整性，避免切断问题-答案关系</li>
 *   <li><b>表格语义保留</b>：提取表头与行数据的 Key-Value 映射关系，支持结构化问答场景</li>
 *   <li><b>可扩展性</b>：通过 CommonMark Extension 机制支持 GFM 语法（表格、删除线、任务列表等）</li>
 * </ul>
 *
 * <h3>解析输出</h3>
 * <p>本解析器提供两种输出模式：</p>
 * <ul>
 *   <li>{@link #parse(InputStream, String)} — 返回纯文本字符串（去除格式标记但保留结构分隔符），用于向量化</li>
 *   <li>{@link #parseToDocument(String)} — 返回完整的 {@link MarkdownDocument} 结构化模型，
 *       包含章节树、表格数据、元数据等，用于智能切片和上下文增强</li>
 * </ul>
 *
 * <h3>分块策略</h3>
 * <p>{@link #parseChunks(InputStream, String, int, int)} 方法采用<strong>优先级回退策略</strong>：</p>
 * <ol>
 *   <li>优先按 Heading 边界进行结构化章节切片（每个 chunk 对应一个完整章节）</li>
 *   <li>当文档无 Heading 时，回退到基于字符数的滑动窗口切片（在句子/段落边界处优雅分割）</li>
 * </ol>
 *
 * <h3>支持的语法</h3>
 * <table border="1">
 *   <tr><th>类别</th><th>语法元素</th></tr>
 *   <tr><td>标题</td><td>ATX (#) 和 Setext (= / -)</td></tr>
 *   <tr><td>列表</td><td>无序 (-, *, +)、有序 (1.)、嵌套列表（最大6级缩进）</td></tr>
 *   <tr><td>代码块</td><td>围栏代码块 (```language)、缩进代码块 (4空格)</td></tr>
 *   <tr><td>表格</td><td>GFM 扩展表格 (| header |)，含对齐方式</td></tr>
 *   <tr><td>引用</td><td>块引用 (&gt; )</td></tr>
 *   <tr><td>链接/图片</td><td>[text](url)、![alt](url)</td></tr>
 *   <tr><td>强调</td><td>粗体 (**text**)、斜体 (*text*)</td></tr>
 *   <tr><td>行内代码</td><td>`code`</td></tr>
 *   <tr><td>分隔线</td><td>---、***、___</td></tr>
 * </table>
 *
 * <h3>线程安全性</h3>
 * <p>CommonMark 的 {@link Parser} 是线程安全的，本类的 {@code parser} 实例可安全地在多线程中复用。</p>
 *
 * @see ParserPlugin
 * @see MarkdownDocument
 * @see RagMarkdownVisitor
 * @see org.commonmark.parser.Parser
 */
@Slf4j
public class MarkdownParserPlugin implements ParserPlugin {

    private final Parser parser = Parser.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();

    private final int minChunkContentLength;

    public MarkdownParserPlugin() {
        this.minChunkContentLength = 50;
    }

    public MarkdownParserPlugin(int minChunkContentLength) {
        this.minChunkContentLength = minChunkContentLength;
    }

    /**
     * 返回解析器名称标识。
     *
     * @return 固定值 "markdown-parser-ast"
     */
    @Override
    public String getName() {
        return "markdown-parser-ast";
    }

    /**
     * 返回本解析器支持的文件格式列表。
     *
     * <p>支持标准 Markdown (.md) 和完整扩展名 (.markdown) 两种格式。</p>
     *
     * @return 包含 "md" 和 "markdown" 的不可变列表
     */
    @Override
    public List<String> getSupportedFormats() {
        return List.of("md", "markdown");
    }

    /**
     * 将 Markdown 输入流解析为纯文本字符串。
     *
     * <p>内部流程：读取输入流 → 构建 AST → 遍历提取纯文本（去除格式标记但保留结构分隔符）。
     * 纯文本适用于向量化、语义检索等 RAG 场景。</p>
     *
     * @param inputStream  Markdown 文件的输入流（UTF-8 编码）
     * @param filename     文件名（用于日志记录）
     * @return 去除所有 Markdown 格式标记后的纯文本内容
     * @throws RuntimeException 当 IO 读取或解析过程发生错误时抛出
     */
    @Override
    public String parse(InputStream inputStream, String filename) {
        try {
            String markdownContent = readInputStream(inputStream);
            MarkdownDocument document = parseToDocument(markdownContent);
            document.setRawText(markdownContent);
            return document.getPlainText();
        } catch (IOException e) {
            log.error("Markdown解析失败: {}", filename, e);
            throw new RuntimeException("Markdown解析失败", e);
        }
    }

    /**
     * 将 Markdown 输入流解析为文本分块列表，采用<strong>结构化优先</strong>的分块策略。
     *
     * <p>分块策略（按优先级）：</p>
     * <ol>
     *   <li><b>结构化章节切片</b>：当文档包含 Heading 时，每个 Heading 下的内容作为一个独立 chunk，
     *       保证语义单元完整性（不会将问题切在 chunk A 而答案切在 chunk B）</li>
     *   <li><b>字符数回退切片</b>：当文档无 Heading 或章节内容过长时，
     *       回退到基于 {@code chunkSize} 和 {@code overlap} 的滑动窗口切片，
     *       在句子/段落边界处优雅分割</li>
     * </ol>
     *
     * @param inputStream  Markdown 文件的输入流
     * @param filename     文件名
     * @param chunkSize    单个分块的最大字符数（仅回退策略时生效）
     * @param overlap      相邻分块之间的重叠字符数（仅回退策略时生效）
     * @return 分块后的文本字符串列表
     */
    @Override
    public List<String> parseChunks(InputStream inputStream, String filename, int chunkSize, int overlap) {
        try {
            String markdownContent = readInputStream(inputStream);
            MarkdownDocument document = parseToDocument(markdownContent);

            List<MarkdownSection> sections = document.getSections();
            if (sections != null && !sections.isEmpty()) {
                log.debug("使用结构化章节切片: 章节数={}", sections.size());
                List<String> sectionChunks = flattenSections(sections);
                List<String> result = enforceChunkSizeLimit(sectionChunks, chunkSize, overlap);
                return filterShortChunks(result);
            }

            log.debug("回退到字符数切片");
            List<String> chunks = splitIntoChunks(document.getPlainText(), chunkSize, overlap);
            return filterShortChunks(chunks);
        } catch (IOException e) {
            log.error("Markdown解析失败: {}", filename, e);
            throw new RuntimeException("Markdown解析失败", e);
        }
    }

    private List<String> filterShortChunks(List<String> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return chunks;
        }
        List<String> filtered = new ArrayList<>();
        for (String chunk : chunks) {
            if (chunk.trim().length() >= minChunkContentLength) {
                filtered.add(chunk);
            } else {
                log.debug("过滤过短chunk: length={}", chunk.length());
            }
        }
        if (filtered.size() < chunks.size()) {
            log.debug("过滤完成: 原始数量={}, 过滤后数量={}", chunks.size(), filtered.size());
        }
        return filtered;
    }

    /**
     * 将 Markdown 文本解析为完整的结构化文档模型（AST 高层封装）。
     *
     * <p>与 {@link #parse(InputStream, String)} 不同，本方法返回的 {@link MarkdownDocument}
     * 包含完整的结构化信息：</p>
     * <ul>
     *   <li>{@code plainText} — 纯文本内容</li>
     *   <li>{@code sections} — 按 Heading 层级组织的章节树（支持嵌套）</li>
     *   <li>{@code tables} — 结构化表格数据（表头 + Key-Value 行映射）</li>
     *   <li>{@code headings} — 所有标题的扁平列表</li>
     *   <li>{@code codeBlockCount} — 代码块数量统计</li>
     * </ul>
     *
     * <p>适用于需要结构化信息的场景：智能切片、上下文增强、文档质量评估等。</p>
     *
     * @param markdown 原始 Markdown 文本
     * @return 包含纯文本、章节树、表格数据的完整文档模型
     */
    public MarkdownDocument parseToDocument(String markdown) {
        org.commonmark.node.Node astRoot = parser.parse(markdown);
        
        RagMarkdownVisitor visitor = new RagMarkdownVisitor();
        astRoot.accept(visitor);
        
        MarkdownDocument document = visitor.getDocument();
        document.setRawText(markdown);
        
        return document;
    }

    /**
     * 将 Markdown 输入流解析为完整的结构化文档模型。
     *
     * <p>便捷方法，等价于先调用 {@link #readInputStream(InputStream)} 再调用
     * {@link #parseToDocument(String)}。</p>
     *
     * @param inputStream  Markdown 文件的输入流（UTF-8 编码）
     * @param filename     文件名（用于日志记录）
     * @return 包含纯文本、章节树、表格数据的完整文档模型
     * @throws IOException 当读取输入流失败时抛出
     */
    public MarkdownDocument parseToDocument(InputStream inputStream, String filename) throws IOException {
        String markdownContent = readInputStream(inputStream);
        return parseToDocument(markdownContent);
    }

    /**
     * 从输入流中读取全部文本内容（UTF-8 编码）。
     *
     * @param inputStream 输入流
     * @return 完整的文本内容，保留原始换行符
     * @throws IOException 当读取失败时抛出
     */
    private String readInputStream(InputStream inputStream) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    /**
     * 将章节树递归展平为文本分块列表。
     *
     * <p>每个有内容的章节生成一个 chunk，格式为 "标题\n\n内容"。
     * 子章节在父章节之后依次排列。</p>
     *
     * @param sections 章节列表（可能包含嵌套的子章节）
     * @return 展平后的文本分块列表
     */
    private List<String> flattenSections(List<MarkdownSection> sections) {
        List<String> chunks = new ArrayList<>();
        for (MarkdownSection section : sections) {
            String sectionContent = section.getContent();
            if (section.hasContent() && !isPureHeadingChunk(section.getTitle(), sectionContent)) {
                chunks.add(sectionContent);
            }

            if (section.getSubSections() != null && !section.getSubSections().isEmpty()) {
                chunks.addAll(flattenSections(section.getSubSections()));
            }
        }
        return chunks;
    }

    private boolean isPureHeadingChunk(String title, String content) {
        if (title == null || content == null) {
            return false;
        }
        String trimmedContent = content.trim();
        String trimmedTitle = title.trim();
        return trimmedContent.equals(trimmedTitle) ||
               trimmedContent.equals(trimmedTitle + "\n\n" + trimmedTitle);
    }

    /**
     * 强制执行分块大小限制——对超长 chunk 进行二次切分。
     *
     * <p>遍历所有分块，对长度超过 {@code chunkSize} 的 chunk 调用
     * {@link #splitIntoChunks(String, int, int)} 进行二次分割，
     * 未超限的 chunk 原样保留。</p>
     *
     * @param chunks   原始分块列表
     * @param chunkSize 单个分块的最大字符数
     * @param overlap   相邻分块之间的重叠字符数
     * @return 保证每个 chunk 长度不超过 chunkSize 的分块列表
     */
    private List<String> enforceChunkSizeLimit(List<String> chunks, int chunkSize, int overlap) {
        if (chunks == null || chunks.isEmpty()) {
            return chunks;
        }
        
        boolean needsProcessing = false;
        for (String chunk : chunks) {
            if (chunk.length() > chunkSize) {
                needsProcessing = true;
                break;
            }
        }
        
        if (!needsProcessing) {
            return chunks;
        }
        
        List<String> result = new ArrayList<>();
        for (String chunk : chunks) {
            if (chunk.length() > chunkSize) {
                log.debug("章节超长({} > {})，进行二次切分", chunk.length(), chunkSize);
                result.addAll(splitIntoChunks(chunk, chunkSize, overlap));
            } else {
                result.add(chunk);
            }
        }
        return result;
    }

    /**
     * 基于滑动窗口的文本分块算法，在语义边界处优雅分割。
     *
     * <p>分割优先级（从高到低）：双换行 → 单换行 → 句号/感叹号/问号（中英文）→ 空格/制表符。
     * 当找不到语义分割点时，在 {@code end} 位置硬切。</p>
     *
     * @param text      待分块的文本
     * @param chunkSize 每个分块的最大字符数
     * @param overlap    相邻分块之间的重叠字符数
     * @return 分块后的文本列表
     */
    private List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        int length = text.length();
        if (length <= chunkSize) {
            chunks.add(text.trim());
            return chunks;
        }

        int start = 0;
        int lastStart = -1;
        int maxIterations = length / Math.max(1, chunkSize - overlap) + 10;
        int iteration = 0;

        while (start < length && iteration < maxIterations) {
            iteration++;
            
            if (start == lastStart) {
                start = Math.min(start + 1, length);
                lastStart = start;
                continue;
            }
            lastStart = start;

            int end = Math.min(start + chunkSize, length);
            if (end < length) {
                int lastSplit = findGoodSplitPoint(text, start, end);
                if (lastSplit > start) {
                    end = lastSplit;
                }
            }
            int newStart = start;
            while (newStart < end && Character.isWhitespace(text.charAt(newStart))) {
                newStart++;
            }
            int newEnd = end;
            while (newEnd > newStart && Character.isWhitespace(text.charAt(newEnd - 1))) {
                newEnd--;
            }
            if (newStart < newEnd) {
                String chunk = text.substring(newStart, newEnd);
                chunks.add(chunk);
            }
            
            if (end >= length) {
                break;
            }
            
            start = end - overlap;
            if (start < 0) start = 0;
            if (start <= newStart) {
                start = newStart + 1;
            }
        }

        return chunks;
    }

    /**
     * 在指定区间 [start, end] 内查找最佳语义分割点。
     *
     * <p>从 {@code end} 位置向前搜索，按优先级匹配分隔符：
     * {@code \n\n} → {@code \n} → 句号/感叹号/问号（中英文）→ 空格/制表符。</p>
     *
     * @param text 待搜索的文本
     * @param start 搜索区间的起始位置（含）
     * @param end   搜索区间的结束位置（含）
     * @return 最佳分割点的位置索引；若未找到则返回 {@code end}
     */
    private int findGoodSplitPoint(String text, int start, int end) {
        String[] separators = {"\n\n", "\n", ". ", "! ", "? ", "。", "！", "？", " ", "\t"};
        for (String separator : separators) {
            int lastIndex = text.lastIndexOf(separator, end);
            if (lastIndex > start) {
                return lastIndex + separator.length();
            }
        }
        return end;
    }
}
