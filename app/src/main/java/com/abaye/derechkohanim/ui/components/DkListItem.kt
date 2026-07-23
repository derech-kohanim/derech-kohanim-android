package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.abaye.derechkohanim.data.model.MatchedRange
import com.abaye.derechkohanim.ui.theme.DkTheme

/**
 * One suggestion row: a category icon, a title that stands alone, a quieter subtitle, and
 * the distance when it is known. The whole row is the touch target.
 */
@Composable
fun DkListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    matchedRanges: List<MatchedRange> = emptyList(),
    leadingIcon: ImageVector? = null,
    trailingText: String? = null,
    trailingIcon: ImageVector? = null,
    trailingIconDescription: String? = null,
    /** When set, the trailing icon is its own button; otherwise it is decorative. */
    onTrailingClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = DkTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = DkTheme.touch.comfortableTarget)
            .padding(horizontal = DkTheme.spacing.s4, vertical = DkTheme.spacing.s2),
    ) {
        if (leadingIcon != null) {
            Surface(
                shape = CircleShape,
                color = colors.surfaceMuted,
                contentColor = colors.textMuted,
                modifier = Modifier.size(DkTheme.touch.minTarget),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = leadingIcon, contentDescription = null)
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = DkTheme.spacing.s3),
        ) {
            HighlightedText(
                text = title,
                ranges = matchedRanges,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.text,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
            )
        }
        if (trailingIcon != null) {
            if (onTrailingClick != null) {
                DkIconButton(
                    icon = trailingIcon,
                    contentDescription = trailingIconDescription,
                    onClick = onTrailingClick,
                    tint = colors.textMuted,
                )
            } else {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = trailingIconDescription,
                    tint = colors.textMuted,
                )
            }
        }
    }
}
