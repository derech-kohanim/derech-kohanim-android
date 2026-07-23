package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.abaye.derechkohanim.ui.theme.DkTheme

/**
 * Grouping surface. Separation comes from the surface colour and a hairline border rather
 * than a shadow, because the token set carries no elevation values for Compose yet.
 */
@Composable
fun DkCard(
    modifier: Modifier = Modifier,
    color: Color = DkTheme.colors.surface,
    contentColor: Color = DkTheme.colors.text,
    borderColor: Color = DkTheme.colors.border,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(DkTheme.radius.lg)
    val border = BorderStroke(Dp.Hairline, borderColor)
    val body: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(DkTheme.spacing.s4), content = content)
    }

    if (onClick == null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            border = border,
        ) { body() }
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            border = border,
        ) { body() }
    }
}
