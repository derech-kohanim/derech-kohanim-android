package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import com.abaye.derechkohanim.ui.theme.DkTheme

/** One cell: its label, the number, and an optional unit drawn smaller in the muted colour. */
data class DkStat(val label: String, val value: String, val unit: String? = null)

/**
 * Civic data table: a row of cells split by hairlines, each with its label ABOVE the number.
 * Numbers are regular weight; size carries them. [bordered] draws the outer box (route review);
 * the driving bar sits flush in its own surface and leaves it off.
 */
@Composable
fun DkStatTable(
    stats: List<DkStat>,
    modifier: Modifier = Modifier,
    bordered: Boolean = true,
    valueStyle: TextStyle = MaterialTheme.typography.displaySmall,
) {
    val colors = DkTheme.colors
    val content: @Composable () -> Unit = {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            stats.forEachIndexed { index, stat ->
                if (index > 0) {
                    VerticalDivider(modifier = Modifier.fillMaxHeight(), thickness = Dp.Hairline, color = colors.border)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = DkTheme.spacing.s3, vertical = DkTheme.spacing.s2),
                ) {
                    Text(text = stat.label, style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                    Text(
                        text = buildAnnotatedString {
                            append(stat.value)
                            stat.unit?.let { unit ->
                                append(' ')
                                withStyle(SpanStyle(fontSize = MaterialTheme.typography.labelMedium.fontSize, color = colors.textMuted)) {
                                    append(unit)
                                }
                            }
                        },
                        style = valueStyle,
                        color = colors.text,
                        maxLines = 1,
                    )
                }
            }
        }
    }
    if (bordered) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(DkTheme.radius.md),
            color = colors.surface,
            border = BorderStroke(Dp.Hairline, colors.border),
        ) { content() }
    } else {
        Box0(modifier) { content() }
    }
}

@Composable
private fun Box0(modifier: Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxWidth()) { content() }
}
