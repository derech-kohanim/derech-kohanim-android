package com.abaye.derechkohanim.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Every token colour, including the ones Material 3 has no slot for (zone, routeNaive,
 * textMuted, focus). Read it through [DkTheme.colors] so nothing has to know which
 * palette is active.
 */
@Immutable
data class DkColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val surfaceOverlay: Color,
    val border: Color,
    val text: Color,
    val textMuted: Color,
    val primary: Color,
    val primaryHover: Color,
    val primaryContrast: Color,
    val accent: Color,
    val accentContrast: Color,
    val success: Color,
    val successContrast: Color,
    val danger: Color,
    val dangerContrast: Color,
    val warning: Color,
    val warningContrast: Color,
    val zone: Color,
    val zoneBasic: Color,
    val zoneTaharat: Color,
    val zoneMachmirim: Color,
    val routeNaive: Color,
    val focus: Color,
    val isDark: Boolean,
)

internal val dkLightColors = DkColors(
    background = LightColors.background,
    surface = LightColors.surface,
    surfaceMuted = LightColors.surfaceMuted,
    surfaceOverlay = LightColors.surfaceOverlay,
    border = LightColors.border,
    text = LightColors.text,
    textMuted = LightColors.textMuted,
    primary = LightColors.primary,
    primaryHover = LightColors.primaryHover,
    primaryContrast = LightColors.primaryContrast,
    accent = LightColors.accent,
    accentContrast = LightColors.accentContrast,
    success = LightColors.success,
    successContrast = LightColors.successContrast,
    danger = LightColors.danger,
    dangerContrast = LightColors.dangerContrast,
    warning = LightColors.warning,
    warningContrast = LightColors.warningContrast,
    zone = LightColors.zone,
    zoneBasic = LightColors.zoneBasic,
    zoneTaharat = LightColors.zoneTaharat,
    zoneMachmirim = LightColors.zoneMachmirim,
    routeNaive = LightColors.routeNaive,
    focus = LightColors.focus,
    isDark = false,
)

internal val dkDarkColors = DkColors(
    background = DarkColors.background,
    surface = DarkColors.surface,
    surfaceMuted = DarkColors.surfaceMuted,
    surfaceOverlay = DarkColors.surfaceOverlay,
    border = DarkColors.border,
    text = DarkColors.text,
    textMuted = DarkColors.textMuted,
    primary = DarkColors.primary,
    primaryHover = DarkColors.primaryHover,
    primaryContrast = DarkColors.primaryContrast,
    accent = DarkColors.accent,
    accentContrast = DarkColors.accentContrast,
    success = DarkColors.success,
    successContrast = DarkColors.successContrast,
    danger = DarkColors.danger,
    dangerContrast = DarkColors.dangerContrast,
    warning = DarkColors.warning,
    warningContrast = DarkColors.warningContrast,
    zone = DarkColors.zone,
    zoneBasic = DarkColors.zoneBasic,
    zoneTaharat = DarkColors.zoneTaharat,
    zoneMachmirim = DarkColors.zoneMachmirim,
    routeNaive = DarkColors.routeNaive,
    focus = DarkColors.focus,
    isDark = true,
)

val LocalDkColors = staticCompositionLocalOf { dkLightColors }
