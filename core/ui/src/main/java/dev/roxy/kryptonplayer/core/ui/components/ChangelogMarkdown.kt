package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/**
 * Known update changelog categories with Mochy-inspired badges and tinted color palettes.
 */
enum class ChangelogCategory(
    val title: String,
    val emoji: String,
    val icon: ImageVector?,
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color,
) {
    FEATURES(
        title = "Features",
        emoji = "🚀",
        icon = Icons.Rounded.RocketLaunch,
        containerColor = Color(0x2E4CAF50),
        contentColor = Color(0xFF81C784),
        borderColor = Color(0x4D81C784),
    ),
    BUG_FIXES(
        title = "Bug Fixes",
        emoji = "🐛",
        icon = Icons.Rounded.BugReport,
        containerColor = Color(0x2EE57373),
        contentColor = Color(0xFFEF9A9A),
        borderColor = Color(0x4DEF9A9A),
    ),
    PERFORMANCE(
        title = "Performance",
        emoji = "⚡",
        icon = Icons.Rounded.FlashOn,
        containerColor = Color(0x2EFFD54F),
        contentColor = Color(0xFFFFE082),
        borderColor = Color(0x4DFFE082),
    ),
    DESIGN(
        title = "Design & UI",
        emoji = "🎨",
        icon = Icons.Rounded.Palette,
        containerColor = Color(0x2EBA68C8),
        contentColor = Color(0xFFCE93D8),
        borderColor = Color(0x4DCE93D8),
    ),
    IMPROVEMENTS(
        title = "Improvements",
        emoji = "🔧",
        icon = Icons.Rounded.Build,
        containerColor = Color(0x2E4FC3F7),
        contentColor = Color(0xFF81D4FA),
        borderColor = Color(0x4D81D4FA),
    ),
    SECURITY(
        title = "Security",
        emoji = "🛡️",
        icon = Icons.Rounded.Security,
        containerColor = Color(0x2E4DB6AC),
        contentColor = Color(0xFF80CBC4),
        borderColor = Color(0x4D80CBC4),
    ),
    DEPENDENCIES(
        title = "Dependencies",
        emoji = "📦",
        icon = Icons.Rounded.ViewAgenda,
        containerColor = Color(0x2EFFB74D),
        contentColor = Color(0xFFFFCC80),
        borderColor = Color(0x4DFFCC80),
    ),
    DOCUMENTATION(
        title = "Docs",
        emoji = "📝",
        icon = Icons.Rounded.Description,
        containerColor = Color(0x2E90A4AE),
        contentColor = Color(0xFFB0BEC5),
        borderColor = Color(0x4DB0BEC5),
    );

    companion object {
        fun matchFrom(text: String): ChangelogCategory? {
            val lower = text.lowercase()
            return when {
                lower.contains("🚀") || lower.contains("feature") || lower.contains("what's new") -> FEATURES
                lower.contains("🐛") || lower.contains("bug") || lower.contains("fix") -> BUG_FIXES
                lower.contains("⚡") || lower.contains("performance") || lower.contains("speed") -> PERFORMANCE
                lower.contains("🎨") || lower.contains("design") || lower.contains("ui") || lower.contains("ux") -> DESIGN
                lower.contains("🔧") || lower.contains("improvement") || lower.contains("refactor") || lower.contains("chore") -> IMPROVEMENTS
                lower.contains("🛡️") || lower.contains("security") -> SECURITY
                lower.contains("📦") || lower.contains("dependenc") -> DEPENDENCIES
                lower.contains("📝") || lower.contains("doc") -> DOCUMENTATION
                else -> null
            }
        }
    }
}

/**
 * Expressive category tag badge for release notes.
 */
@Composable
fun ChangelogCategoryBadge(
    category: ChangelogCategory,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = category.containerColor,
        border = BorderStroke(1.dp, category.borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = category.emoji,
                fontSize = 12.sp,
            )
            Text(
                text = category.title,
                color = category.contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
            )
        }
    }
}

private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class ListItem(
        val marker: String,
        val text: String,
        val indent: Int,
        val isTask: Boolean = false,
        val isChecked: Boolean = false,
    ) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val text: String) : MdBlock
    data class Image(val url: String, val alt: String) : MdBlock
    data object Rule : MdBlock
}

private val MdImage = Regex("""!\[([^\]]*)]\(\s*<?([^)\s>]+)>?(?:\s+"[^"]*")?\s*\)""")
private val HtmlImage = Regex("""<img\b[^>]*?\bsrc\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
private val HtmlAlt = Regex("""\balt\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
private val HtmlTag = Regex("""</?[a-zA-Z][^>]*>""")
private val HeadingLine = Regex("""^(#{1,6})\s+(.*?)\s*#*\s*$""")
private val BulletLine = Regex("""^(\s*)[-*+]\s+(?:\[([ xX])]\s+)?(.*)$""")
private val NumberLine = Regex("""^(\s*)(\d+)[.)]\s+(.*)$""")
private val RuleLine = Regex("""^\s*([-*_])(\s*\1){2,}\s*$""")
private val Fence = Regex("""^\s*(```|~~~)""")

/**
 * Pulls image references out of markdown line, returning text left over plus the images in order.
 */
private fun extractImages(line: String): Pair<String, List<MdBlock.Image>> {
    val images = mutableListOf<Pair<Int, MdBlock.Image>>()
    MdImage.findAll(line).forEach { images += it.range.first to MdBlock.Image(it.groupValues[2], it.groupValues[1]) }
    HtmlImage.findAll(line).forEach { match ->
        val alt = HtmlAlt.find(match.value)?.groupValues?.get(1).orEmpty()
        images += match.range.first to MdBlock.Image(match.groupValues[1], alt)
    }
    if (images.isEmpty()) return line to emptyList()
    val rest = line.replace(MdImage, " ").replace(HtmlImage, " ")
    return rest to images.sortedBy { it.first }.map { it.second }
}

private fun parseBlocks(markdown: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val paragraph = StringBuilder()

    fun flushParagraph() {
        val text = paragraph.toString().trim()
        if (text.isNotEmpty()) blocks += MdBlock.Paragraph(text)
        paragraph.clear()
    }

    val lines = markdown.replace("\r\n", "\n").replace('\r', '\n').lines()
    var i = 0
    while (i < lines.size) {
        val raw = lines[i]

        if (Fence.containsMatchIn(raw)) {
            flushParagraph()
            val code = StringBuilder()
            i++
            while (i < lines.size && !Fence.containsMatchIn(lines[i])) {
                if (code.isNotEmpty()) code.append('\n')
                code.append(lines[i])
                i++
            }
            blocks += MdBlock.Code(code.toString())
            i++
            continue
        }

        // Strip HTML comments (e.g. template artifacts) and standard tags
        val (withoutImages, images) = extractImages(raw.replace(Regex("<!--.*?-->"), ""))
        val line = withoutImages.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), " ")
            .replace(HtmlTag, "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")

        when {
            line.isBlank() -> flushParagraph()
            RuleLine.matches(line) -> {
                flushParagraph()
                blocks += MdBlock.Rule
            }
            HeadingLine.matches(line) -> {
                flushParagraph()
                val match = HeadingLine.find(line)!!
                blocks += MdBlock.Heading(match.groupValues[1].length, match.groupValues[2])
            }
            BulletLine.matches(line) -> {
                flushParagraph()
                val match = BulletLine.find(line)!!
                val taskChar = match.groupValues[2]
                val isTask = taskChar.isNotEmpty()
                val isChecked = isTask && taskChar.lowercase() == "x"
                blocks += MdBlock.ListItem(
                    marker = "•",
                    text = match.groupValues[3].trim(),
                    indent = match.groupValues[1].length / 2,
                    isTask = isTask,
                    isChecked = isChecked,
                )
            }
            NumberLine.matches(line) -> {
                flushParagraph()
                val match = NumberLine.find(line)!!
                blocks += MdBlock.ListItem(
                    marker = "${match.groupValues[2]}.",
                    text = match.groupValues[3].trim(),
                    indent = match.groupValues[1].length / 2,
                )
            }
            line.trimStart().startsWith(">") -> {
                flushParagraph()
                blocks += MdBlock.Quote(line.trimStart().removePrefix(">").trim())
            }
            else -> {
                val last = blocks.lastOrNull()
                if (paragraph.isEmpty() && last is MdBlock.ListItem && raw.startsWith("  ")) {
                    blocks[blocks.lastIndex] = last.copy(text = last.text + " " + line.trim())
                } else {
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(line.trim())
                }
            }
        }

        if (images.isNotEmpty()) {
            flushParagraph()
            blocks += images
        }
        i++
    }
    flushParagraph()
    return blocks
}

private val InlinePattern = Regex(
    """(\*\*|__)(.+?)\1""" + // 1-2: bold
        """|~~(.+?)~~""" + // 3: strike
        """|`([^`]+)`""" + // 4: inline code chip
        """|\[([^\]]+)]\(\s*<?([^)\s>]+)>?(?:\s+"[^"]*")?\s*\)""" + // 5-6: link
        """|(?<![\w*])\*(?![\s*])(.+?)(?<![\s*])\*(?![\w*])""" + // 7: *italic*
        """|(?<![\w_])_(?![\s_])(.+?)(?<![\s_])_(?![\w_])""" + // 8: _italic_
        """|(https?://[^\s<>()]+[^\s<>().,;:!?'"])""", // 9: bare link
)

private fun AnnotatedString.Builder.appendInline(
    text: String,
    strongStyle: SpanStyle,
    linkStyle: SpanStyle,
    codeStyle: SpanStyle,
) {
    var cursor = 0
    while (cursor < text.length) {
        val match = InlinePattern.find(text, cursor)
        if (match == null) {
            append(text.substring(cursor))
            return
        }
        append(text.substring(cursor, match.range.first))
        val g = match.groupValues
        when {
            g[2].isNotEmpty() -> withStyle(strongStyle) { appendInline(g[2], strongStyle, linkStyle, codeStyle) }
            g[3].isNotEmpty() -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                appendInline(g[3], strongStyle, linkStyle, codeStyle)
            }
            g[4].isNotEmpty() -> withStyle(codeStyle) {
                append(" ${g[4]} ")
            }
            g[5].isNotEmpty() -> withLink(LinkAnnotation.Url(g[6], TextLinkStyles(style = linkStyle))) {
                appendInline(g[5], strongStyle, linkStyle, codeStyle)
            }
            g[7].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { appendInline(g[7], strongStyle, linkStyle, codeStyle) }
            g[8].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { appendInline(g[8], strongStyle, linkStyle, codeStyle) }
            g[9].isNotEmpty() -> withLink(LinkAnnotation.Url(g[9], TextLinkStyles(style = linkStyle))) { append(g[9]) }
        }
        cursor = match.range.last + 1
    }
}

private fun inline(
    text: String,
    strongColor: Color,
    linkColor: Color,
    codeBackground: Color,
    codeTextColor: Color,
): AnnotatedString = buildAnnotatedString {
    appendInline(
        text = text,
        strongStyle = SpanStyle(fontWeight = FontWeight.Bold, color = strongColor),
        linkStyle = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.SemiBold),
        codeStyle = SpanStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.5.sp,
            color = codeTextColor,
            background = codeBackground,
            fontWeight = FontWeight.Medium,
        ),
    )
}

/**
 * Mochy-inspired Compose Markdown renderer for Krypton Player changelogs.
 *
 * Supports:
 * - Expressive Headers (H1, H2, H3) with automatic category badge highlights
 * - Bullet lists, numbered lists, nested indentation, and task checkboxes
 * - Bold, italic, strikethrough, inline monospace code chips, and links
 * - Blockquotes with styled accent pill border
 * - Fenced code blocks with monospace font and dark container
 * - Image embedding via Coil
 */
@Composable
fun ChangelogMarkdown(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    strongColor: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    codeBackground: Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
    codeTextColor: Color = MaterialTheme.colorScheme.primary,
    fontSize: TextUnit = 13.sp,
    lineHeight: TextUnit = 19.sp,
    emptyText: String = "No changelog provided for this release.",
) {
    val blocks = remember(markdown) { parseBlocks(markdown.trim()) }

    if (blocks.isEmpty()) {
        Text(
            text = emptyText,
            color = color.copy(alpha = 0.6f),
            fontSize = fontSize,
            lineHeight = lineHeight,
            modifier = modifier.padding(vertical = 8.dp),
        )
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> {
                    val category = ChangelogCategory.matchFrom(block.text)
                    Column(
                        modifier = Modifier.padding(top = if (block.level == 1) 12.dp else 6.dp, bottom = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (category != null && block.level <= 3) {
                            ChangelogCategoryBadge(category = category)
                        }
                        Text(
                            text = remember(block, strongColor, linkColor, codeBackground) {
                                inline(block.text, strongColor, linkColor, codeBackground, codeTextColor)
                            },
                            color = strongColor,
                            fontSize = when (block.level) {
                                1 -> 18.sp
                                2 -> 16.sp
                                else -> 14.5.sp
                            },
                            lineHeight = when (block.level) {
                                1 -> 24.sp
                                2 -> 22.sp
                                else -> 20.sp
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                is MdBlock.Paragraph -> {
                    Text(
                        text = remember(block, strongColor, linkColor, codeBackground) {
                            inline(block.text, strongColor, linkColor, codeBackground, codeTextColor)
                        },
                        color = color,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                    )
                }

                is MdBlock.ListItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (block.indent * 14).dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        if (block.isTask) {
                            Icon(
                                imageVector = if (block.isChecked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (block.isChecked) MaterialTheme.colorScheme.primary else color.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .padding(top = 2.dp, end = 6.dp)
                                    .size(16.dp),
                            )
                        } else {
                            Text(
                                text = block.marker,
                                color = color.copy(alpha = 0.8f),
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(if (block.marker == "•") 14.dp else 22.dp),
                            )
                        }
                        Text(
                            text = remember(block, strongColor, linkColor, codeBackground) {
                                inline(block.text, strongColor, linkColor, codeBackground, codeTextColor)
                            },
                            color = color,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }

                is MdBlock.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .heightIn(min = 20.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = remember(block, strongColor, linkColor, codeBackground) {
                                inline(block.text, strongColor, linkColor, codeBackground, codeTextColor)
                            },
                            color = color.copy(alpha = 0.9f),
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                }

                is MdBlock.Code -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Code,
                                contentDescription = null,
                                tint = strongColor.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = block.text,
                                color = strongColor.copy(alpha = 0.95f),
                                fontSize = fontSize * 0.92f,
                                lineHeight = lineHeight,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }

                is MdBlock.Image -> {
                    AsyncImage(
                        model = block.url,
                        contentDescription = block.alt.ifBlank { null },
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                }

                MdBlock.Rule -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(1.dp)
                            .background(color.copy(alpha = 0.2f)),
                    )
                }
            }
        }
    }
}

/**
 * Extracts the first meaningful line of a release body as plain text,
 * ideal for notification previews and compact single-line summaries.
 */
fun changelogSummaryLine(markdown: String): String {
    return parseBlocks(markdown).firstNotNullOfOrNull { block ->
        when (block) {
            is MdBlock.Heading -> block.text
            is MdBlock.Paragraph -> block.text
            is MdBlock.ListItem -> block.text
            is MdBlock.Quote -> block.text
            else -> null
        }?.let {
            inline(
                text = it,
                strongColor = Color.Unspecified,
                linkColor = Color.Unspecified,
                codeBackground = Color.Unspecified,
                codeTextColor = Color.Unspecified,
            ).text.trim()
        }?.takeIf { it.isNotEmpty() }
    }.orEmpty()
}
