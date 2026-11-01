package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.abaye.derechkohanim.ui.theme.DkTheme

enum class DkBadgeTone { Neutral, Success, Danger, Warning, Info }

/**
 * Status pill on a soft tint of its tone (Civic chip). Every tone pairs a colour with an icon, because critical information must
 * never rely on colour alone (spec 17.8).
 */
@Composable
fun DkBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: DkBadgeTone = DkBadgeTone.Neutral,
    icon: ImageVector? = null,
) {
    val colors = DkTheme.colors
    val container = when (tone) {
        DkBadgeTone.Neutral -> colors.surfaceMuted
        DkBadgeTone.Success -> colors.successSoft
        DkBadgeTone.Danger -> colors.dangerSoft
        DkBadgeTone.Warning -> colors.warning
        DkBadgeTone.Info -> colors.primarySoft
    }
    val content = when (tone) {
        DkBadgeTone.Neutral -> colors.textMuted
        DkBadgeTone.Success -> colors.success
        DkBadgeTone.Danger -> colors.danger
        DkBadgeTone.Warning -> colors.warningContrast
        DkBadgeTone.Info -> colors.primary
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(DkTheme.radius.full),
        color = container,
        contentColor = content,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = DkTheme.spacing.s3,
                vertical = DkTheme.spacing.s1,
            ),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(DkTheme.spacing.s4),
                )
                Spacer(Modifier.width(DkTheme.spacing.s1))
            }
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}
