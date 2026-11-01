// Generated from derech-kohanim-shared/src/design/tokens.ts. Do not edit.
package com.abaye.derechkohanim.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object LightColors {
    val background: Color = Color(0xFFFFFFFF)
    val surface: Color = Color(0xFFFFFFFF)
    val surfaceMuted: Color = Color(0xFFF6F8FE)
    val band: Color = Color(0xFFF6F8FE)
    val surfaceOverlay: Color = Color(0xFFFFFFFF)
    val border: Color = Color(0xFFE4E9F1)
    val borderStrong: Color = Color(0xFF6D87AB)
    val text: Color = Color(0xFF141C29)
    val textBody: Color = Color(0xFF232934)
    val textMuted: Color = Color(0xFF4D6B9D)
    val primary: Color = Color(0xFF3063D9)
    val primaryHover: Color = Color(0xFF264CA6)
    val primaryContrast: Color = Color(0xFFFFFFFF)
    val primarySoft: Color = Color(0xFFECF1FE)
    val accent: Color = Color(0xFF8C0CA6)
    val accentContrast: Color = Color(0xFFFFFFFF)
    val success: Color = Color(0xFF106027)
    val successContrast: Color = Color(0xFFFFFFFF)
    val successSoft: Color = Color(0xFFE7F3E7)
    val danger: Color = Color(0xFFB42318)
    val dangerContrast: Color = Color(0xFFFFFFFF)
    val dangerSoft: Color = Color(0xFFFDEAE8)
    val warning: Color = Color(0xFFF79F08)
    val warningContrast: Color = Color(0xFF39240E)
    val zone: Color = Color(0xFFDC2834)
    val zoneBasic: Color = Color(0xFFE6378F)
    val zoneTaharat: Color = Color(0xFF955D37)
    val zoneMachmirim: Color = Color(0xFFF58514)
    val zoneDanger: Color = Color(0xFF893BCE)
    val routeNaive: Color = Color(0xFF7E899A)
    val focus: Color = Color(0xFF3063D9)
}

object DarkColors {
    val background: Color = Color(0xFF0E1420)
    val surface: Color = Color(0xFF182235)
    val surfaceMuted: Color = Color(0xFF131C2A)
    val band: Color = Color(0xFF131C2A)
    val surfaceOverlay: Color = Color(0xFF182235)
    val border: Color = Color(0xFF26324A)
    val borderStrong: Color = Color(0xFF6980A5)
    val text: Color = Color(0xFFECF1F8)
    val textBody: Color = Color(0xFFD4DBE7)
    val textMuted: Color = Color(0xFF9EB3D6)
    val primary: Color = Color(0xFF6E96F2)
    val primaryHover: Color = Color(0xFF9DB8F6)
    val primaryContrast: Color = Color(0xFF0E1420)
    val primarySoft: Color = Color(0xFF1B2A4B)
    val accent: Color = Color(0xFFD58BE4)
    val accentContrast: Color = Color(0xFF0E1420)
    val success: Color = Color(0xFF7ED393)
    val successContrast: Color = Color(0xFF0E1420)
    val successSoft: Color = Color(0xFF16321E)
    val danger: Color = Color(0xFFF97167)
    val dangerContrast: Color = Color(0xFF0E1420)
    val dangerSoft: Color = Color(0xFF3A1513)
    val warning: Color = Color(0xFFF9B939)
    val warningContrast: Color = Color(0xFF311F0C)
    val zone: Color = Color(0xFFE74B56)
    val zoneBasic: Color = Color(0xFFED5AA3)
    val zoneTaharat: Color = Color(0xFFBF794A)
    val zoneMachmirim: Color = Color(0xFFF7993B)
    val zoneDanger: Color = Color(0xFFA767E0)
    val routeNaive: Color = Color(0xFF89919F)
    val focus: Color = Color(0xFF6E96F2)
}

object Radius {
    val xs: Dp = 6.dp
    val sm: Dp = 10.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val full: Dp = 999.dp
}

object Space {
    val s0: Dp = 0.dp
    val s1: Dp = 4.dp
    val s2: Dp = 8.dp
    val s3: Dp = 12.dp
    val s4: Dp = 16.dp
    val s5: Dp = 20.dp
    val s6: Dp = 24.dp
    val s8: Dp = 32.dp
    val s10: Dp = 40.dp
    val s12: Dp = 48.dp
    val s16: Dp = 64.dp
}

object FontSize {
    val caption: TextUnit = 12.sp
    val label: TextUnit = 14.sp
    val body: TextUnit = 16.sp
    val bodyLarge: TextUnit = 18.sp
    val title: TextUnit = 21.sp
    val heading: TextUnit = 24.sp
    val display: TextUnit = 32.sp
    val navPrimary: TextUnit = 44.sp
}

object MotionDuration {
    const val instant: Int = 90
    const val fast: Int = 160
    const val normal: Int = 240
    const val slow: Int = 380
}

object Touch {
    val minTarget: Dp = 48.dp
    val comfortableTarget: Dp = 56.dp
}
