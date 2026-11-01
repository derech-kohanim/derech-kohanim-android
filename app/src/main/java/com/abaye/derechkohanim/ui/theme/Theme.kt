package com.abaye.derechkohanim.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.foundation.shape.RoundedCornerShape

private fun materialScheme(c: DkColors) = if (c.isDark) {
    darkColorScheme(
        primary = c.primary,
        onPrimary = c.primaryContrast,
        primaryContainer = c.primarySoft,
        onPrimaryContainer = c.primary,
        secondary = c.accent,
        onSecondary = c.accentContrast,
        tertiary = c.success,
        onTertiary = c.successContrast,
        background = c.background,
        onBackground = c.textBody,
        surface = c.surface,
        onSurface = c.textBody,
        surfaceVariant = c.surfaceMuted,
        onSurfaceVariant = c.textMuted,
        surfaceContainer = c.surfaceOverlay,
        surfaceContainerHigh = c.surfaceOverlay,
        surfaceContainerHighest = c.surfaceMuted,
        errorContainer = c.dangerSoft,
        onErrorContainer = c.danger,
        secondaryContainer = c.primarySoft,
        onSecondaryContainer = c.primary,
        outline = c.borderStrong,
        outlineVariant = c.border,
        error = c.danger,
        onError = c.dangerContrast,
    )
} else {
    lightColorScheme(
        primary = c.primary,
        onPrimary = c.primaryContrast,
        primaryContainer = c.primarySoft,
        onPrimaryContainer = c.primary,
        secondary = c.accent,
        onSecondary = c.accentContrast,
        tertiary = c.success,
        onTertiary = c.successContrast,
        background = c.background,
        onBackground = c.textBody,
        surface = c.surface,
        onSurface = c.textBody,
        surfaceVariant = c.surfaceMuted,
        onSurfaceVariant = c.textMuted,
        surfaceContainer = c.surfaceOverlay,
        surfaceContainerHigh = c.surfaceOverlay,
        surfaceContainerHighest = c.surfaceMuted,
        errorContainer = c.dangerSoft,
        onErrorContainer = c.danger,
        secondaryContainer = c.primarySoft,
        onSecondaryContainer = c.primary,
        outline = c.borderStrong,
        outlineVariant = c.border,
        error = c.danger,
        onError = c.dangerContrast,
    )
}

private val DkShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.xs),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.xl),
)

/**
 * Day and night follow the system by default, per spec 17.2. Dynamic colour is deliberately
 * not used - the palette carries halachic meaning and must not be recoloured by the device.
 */
@Composable
fun DerechKohanimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) dkDarkColors else dkLightColors
    CompositionLocalProvider(LocalDkColors provides colors) {
        MaterialTheme(
            colorScheme = materialScheme(colors),
            typography = DkTypography,
            shapes = DkShapes,
            content = content,
        )
    }
}

object DkTheme {
    val colors: DkColors
        @Composable @ReadOnlyComposable get() = LocalDkColors.current

    /** Spacing accessor. `DkTheme.spacing.s4` and never a raw dp value. */
    val spacing = Space
    val radius = Radius
    val fontSize = FontSize
    val touch = Touch
    val motion = MotionDuration
}
