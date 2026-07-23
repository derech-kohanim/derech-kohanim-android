package com.abaye.derechkohanim.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.abaye.derechkohanim.data.model.MatchedRange
import com.abaye.derechkohanim.ui.theme.DkTheme

/**
 * Renders the matched ranges the server reported. Ranges are validated before use because a
 * bad offset from upstream must not crash the destination list.
 */
fun highlight(
    text: String,
    ranges: List<MatchedRange>,
    highlightColor: Color,
): AnnotatedString = buildAnnotatedString {
    append(text)
    ranges.forEach { range ->
        val start = range.start.coerceIn(0, text.length)
        val end = (range.start + range.length).coerceIn(start, text.length)
        if (end > start) {
            addStyle(
                SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold),
                start,
                end,
            )
        }
    }
}

@Composable
fun HighlightedText(
    text: String,
    ranges: List<MatchedRange>,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = DkTheme.colors.text,
    maxLines: Int = 1,
) {
    Text(
        text = highlight(text, ranges, DkTheme.colors.primary),
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Local matching for saved and recent places, which never round trip to the server. */
fun matchRanges(text: String, query: String): List<MatchedRange> {
    val needle = query.trim()
    if (needle.isEmpty()) return emptyList()
    val index = text.indexOf(needle, ignoreCase = true)
    return if (index < 0) emptyList() else listOf(MatchedRange(index, needle.length))
}
