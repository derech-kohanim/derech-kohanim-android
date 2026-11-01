package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.abaye.derechkohanim.ui.theme.DkTheme

/**
 * A deliberately small Markdown subset: headings, paragraphs, lists, bold, italic, inline code
 * and links. The terms of use documents arrive from the server as Markdown, and a dependency is
 * not worth it for a handful of constructs. Anything unrecognised stays literal text, so a
 * document is never swallowed by a syntax the renderer does not know. Kept in step with the web
 * client's Markdown component so both render the same document the same way.
 */
sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Bullets(val ordered: Boolean, val items: List<String>) : MarkdownBlock
}

private val HEADING = Regex("""^(#{1,6})\s+(.*)$""")
private val BULLET = Regex("""^[-*+]\s+(.*)$""")
private val ORDERED = Regex("""^\d+[.)]\s+(.*)$""")

/** Bold before italic, so `**text**` is not read as an empty emphasis pair. */
private val INLINE = Regex("""\*\*([\s\S]+?)\*\*|\*([^\s*][\s\S]*?)\*|`([^`]+)`|\[([^\]]+)\]\(([^)\s]+)\)""")

private val SAFE_HREF = Regex("""^(https?://|mailto:|tel:)""", RegexOption.IGNORE_CASE)

fun parseMarkdown(source: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = mutableListOf<String>()
    var listOrdered = false
    var listItems = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            // A single newline inside a paragraph is a soft wrap in the source, not a line break.
            blocks += MarkdownBlock.Paragraph(paragraph.joinToString(" "))
            paragraph.clear()
        }
    }

    fun flushList() {
        if (listItems.isNotEmpty()) {
            blocks += MarkdownBlock.Bullets(listOrdered, listItems)
            listItems = mutableListOf()
        }
    }

    for (raw in source.replace("\r\n", "\n").replace('\r', '\n').split('\n')) {
        val line = raw.trim()

        if (line.isEmpty()) {
            flushParagraph()
            flushList()
            continue
        }

        val heading = HEADING.find(line)
        if (heading != null) {
            flushParagraph()
            flushList()
            blocks += MarkdownBlock.Heading(
                level = heading.groupValues[1].length,
                text = heading.groupValues[2].trim(),
            )
            continue
        }

        val bullet = BULLET.find(line)
        val ordered = if (bullet == null) ORDERED.find(line) else null
        if (bullet != null || ordered != null) {
            val isOrdered = ordered != null
            flushParagraph()
            if (listItems.isNotEmpty() && listOrdered != isOrdered) flushList()
            listOrdered = isOrdered
            listItems += (bullet ?: ordered)!!.groupValues[1].trim()
            continue
        }

        flushList()
        paragraph += line
    }

    flushParagraph()
    flushList()
    return blocks
}

/** Only schemes that cannot execute code. An unsafe link degrades to its own text. */
private fun safeHref(href: String): String? = href.takeIf { SAFE_HREF.containsMatchIn(it) }

private fun AnnotatedString.Builder.appendInline(
    text: String,
    linkColor: Color,
    codeBackground: Color,
) {
    var last = 0
    for (match in INLINE.findAll(text)) {
        if (match.range.first > last) append(text.substring(last, match.range.first))
        val groups = match.groupValues
        when {
            match.groups[1] != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendInline(groups[1], linkColor, codeBackground)
            }
            match.groups[2] != null -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                appendInline(groups[2], linkColor, codeBackground)
            }
            match.groups[3] != null -> withStyle(
                SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)
            ) {
                append(groups[3])
            }
            else -> {
                val href = safeHref(groups[5])
                if (href == null) {
                    append(groups[4])
                } else {
                    withLink(
                        LinkAnnotation.Url(
                            url = href,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            ),
                        ),
                    ) {
                        append(groups[4])
                    }
                }
            }
        }
        last = match.range.last + 1
    }
    if (last < text.length) append(text.substring(last))
}

private fun inlineText(text: String, linkColor: Color, codeBackground: Color): AnnotatedString =
    buildAnnotatedString { appendInline(text, linkColor, codeBackground) }

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = DkTheme.colors.text,
    /** Number the top-level (## and #) headings "1.", "2.", as the clauses of a document. */
    numberHeadings: Boolean = false,
) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    // The clause number of each top-level heading, by block index.
    val clauseNumbers = remember(blocks, numberHeadings) {
        if (!numberHeadings) {
            emptyMap()
        } else {
            var n = 0
            blocks.withIndex()
                .filter { (_, b) -> b is MarkdownBlock.Heading && b.level <= 2 }
                .associate { (i, _) -> i to ++n }
        }
    }
    val linkColor = DkTheme.colors.primary
    val codeBackground = DkTheme.colors.surfaceMuted

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
    ) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MarkdownBlock.Heading -> Text(
                    text = inlineText(
                        clauseNumbers[index]?.let { "$it. ${block.text}" } ?: block.text,
                        linkColor,
                        codeBackground,
                    ),
                    style = if (block.level <= 2) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.titleSmall
                    },
                    color = DkTheme.colors.text,
                    // Headings belong to the text below them, so they get air above instead.
                    modifier = if (index == 0) {
                        Modifier
                    } else {
                        Modifier.padding(top = DkTheme.spacing.s2)
                    },
                )

                is MarkdownBlock.Paragraph -> Text(
                    text = inlineText(block.text, linkColor, codeBackground),
                    style = style,
                    color = color,
                )

                is MarkdownBlock.Bullets -> Column(
                    verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s1),
                ) {
                    block.items.forEachIndexed { itemIndex, item ->
                        Row {
                            Text(
                                text = if (block.ordered) "${itemIndex + 1}." else "•",
                                style = style,
                                color = color,
                            )
                            Text(
                                text = inlineText(item, linkColor, codeBackground),
                                style = style,
                                color = color,
                                modifier = Modifier.padding(start = DkTheme.spacing.s2),
                            )
                        }
                    }
                }
            }
        }
    }
}
