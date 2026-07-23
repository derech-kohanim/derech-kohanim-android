package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.draw.scale
import com.abaye.derechkohanim.ui.theme.DkTheme

/**
 * Icon action with a touch target that never drops below the token minimum.
 *
 * `mirrorInRtl` is for chrome only, such as a back chevron. Navigation manoeuvre icons must
 * never be mirrored: left and right there are geographic, not directional (spec 14.1).
 */
@Composable
fun DkIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = DkTheme.colors.text,
    size: Dp = DkTheme.touch.minTarget,
    mirrorInRtl: Boolean = false,
) {
    val mirror = mirrorInRtl && LocalLayoutDirection.current == LayoutDirection.Rtl
    IconButton(
        onClick = onClick,
        modifier = modifier.size(size),
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(contentColor = tint),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = if (mirror) Modifier.scale(scaleX = -1f, scaleY = 1f) else Modifier,
        )
    }
}

/** The same action, raised onto its own surface. Used over the map. */
@Composable
fun DkFloatingIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.CircleShape,
        color = DkTheme.colors.surfaceOverlay,
        contentColor = DkTheme.colors.text,
        border = androidx.compose.foundation.BorderStroke(Dp.Hairline, DkTheme.colors.border),
    ) {
        DkIconButton(
            icon = icon,
            contentDescription = contentDescription,
            onClick = onClick,
            size = DkTheme.touch.comfortableTarget,
        )
    }
}
