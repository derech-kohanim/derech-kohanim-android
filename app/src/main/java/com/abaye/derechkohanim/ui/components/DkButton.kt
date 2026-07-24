package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.abaye.derechkohanim.ui.theme.DkTheme
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme

enum class DkButtonVariant { Primary, Secondary, Danger, Ghost }

/**
 * The one button in the product. Height comes from the touch tokens because the driver may
 * be wearing gloves (spec 17.2), and the corner radius from the shape tokens.
 */
@Composable
fun DkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: DkButtonVariant = DkButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    large: Boolean = false,
    leadingIcon: ImageVector? = null,
    /** A multi-colour logo (e.g. the Google G) drawn untinted, unlike [leadingIcon]. */
    leadingPainter: Painter? = null,
) {
    val colors = DkTheme.colors
    val container = when (variant) {
        DkButtonVariant.Primary -> colors.primary
        DkButtonVariant.Secondary -> colors.surfaceMuted
        DkButtonVariant.Danger -> colors.danger
        DkButtonVariant.Ghost -> Color.Transparent
    }
    val content = when (variant) {
        DkButtonVariant.Primary -> colors.primaryContrast
        DkButtonVariant.Secondary -> colors.text
        DkButtonVariant.Danger -> colors.dangerContrast
        DkButtonVariant.Ghost -> colors.primary
    }
    val height = if (large) DkTheme.touch.comfortableTarget else DkTheme.touch.minTarget

    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = height),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(DkTheme.radius.full),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = DISABLED_ALPHA),
            disabledContentColor = content.copy(alpha = DISABLED_ALPHA),
        ),
        contentPadding = PaddingValues(
            horizontal = DkTheme.spacing.s5,
            vertical = DkTheme.spacing.s2,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(DkTheme.spacing.s5),
                color = content,
            )
            Spacer(Modifier.width(DkTheme.spacing.s2))
        } else if (leadingPainter != null) {
            // Drawn as an Image, not an Icon, so a multi-colour logo keeps its own colours.
            Image(
                painter = leadingPainter,
                contentDescription = null,
                modifier = Modifier.size(DkTheme.spacing.s5),
            )
            Spacer(Modifier.width(DkTheme.spacing.s2))
        } else if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null)
            Spacer(Modifier.width(DkTheme.spacing.s2))
        }
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
        )
    }
}

private const val DISABLED_ALPHA = 0.38f

@Preview(name = "Buttons", showBackground = true, locale = "he")
@Composable
private fun DkButtonPreview() {
    DerechKohanimTheme {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DkButton(text = "התחל ניווט", onClick = {}, large = true)
            Spacer(Modifier.width(DkTheme.spacing.s2))
            DkButton(text = "ביטול", onClick = {}, variant = DkButtonVariant.Secondary)
        }
    }
}
