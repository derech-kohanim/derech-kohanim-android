package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.abaye.derechkohanim.ui.theme.DkTheme

/** The tone of a list row's icon chip: royal for ordinary actions, danger for risky ones. */
enum class DkChipTone { Royal, Danger, Neutral }

/**
 * Civic list box: rows separated by hairlines inside one bordered, rounded surface. Add rows
 * with [DkListRow]; dividers between them are drawn by [DkListBox] callers via [DkListDivider].
 */
@Composable
fun DkListBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DkTheme.radius.lg),
        color = DkTheme.colors.surface,
        border = BorderStroke(Dp.Hairline, DkTheme.colors.border),
    ) {
        Column(content = content)
    }
}

@Composable
fun DkListDivider() {
    HorizontalDivider(thickness = Dp.Hairline, color = DkTheme.colors.border)
}

/**
 * One tappable Civic row: an optional icon chip, a title with an optional supporting line, an
 * optional trailing text, and a chevron pointing to the inline end (it opens something).
 */
@Composable
fun DkListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    chipTone: DkChipTone = DkChipTone.Royal,
    supporting: String? = null,
    trailing: String? = null,
    titleColor: Color = DkTheme.colors.text,
    showChevron: Boolean = true,
    /** Drawn instead of the icon chip, e.g. the user's avatar. */
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = DkTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = DkTheme.touch.comfortableTarget)
            .padding(horizontal = DkTheme.spacing.s4, vertical = DkTheme.spacing.s3),
    ) {
        if (leading != null) {
            leading()
        } else if (icon != null) {
            val (bg, fg) = when (chipTone) {
                DkChipTone.Royal -> colors.primarySoft to colors.primary
                DkChipTone.Danger -> colors.dangerSoft to colors.danger
                DkChipTone.Neutral -> colors.surfaceMuted to colors.textMuted
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(DkTheme.spacing.s8)
                    .background(bg, CircleShape),
            ) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(DkTheme.spacing.s5))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            if (supporting != null) {
                Text(text = supporting, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
        }
        if (trailing != null) {
            Text(text = trailing, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
        }
        if (showChevron) {
            // Auto-mirrored: points to the inline end, left in Hebrew and right in English.
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(DkTheme.spacing.s5),
            )
        }
    }
}


/** A section heading above a list box (Civic section style, smaller on a phone). */
@Composable
fun DkSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = DkTheme.colors.text,
        modifier = modifier.padding(bottom = DkTheme.spacing.s2),
    )
}

/**
 * Confirmation before an action that is hard to undo or carries responsibility. The confirm
 * button is royal, or danger when [destructive].
 */
@Composable
fun DkConfirmDialog(
    title: String,
    body: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DkTheme.colors.surface,
        shape = RoundedCornerShape(DkTheme.radius.lg),
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = DkTheme.colors.text) },
        text = { Text(body, style = MaterialTheme.typography.bodyLarge, color = DkTheme.colors.textBody) },
        confirmButton = {
            DkButton(
                text = confirmText,
                onClick = onConfirm,
                variant = if (destructive) DkButtonVariant.Danger else DkButtonVariant.Primary,
            )
        },
        dismissButton = {
            DkButton(text = dismissText, onClick = onDismiss, variant = DkButtonVariant.Ghost)
        },
    )
}
