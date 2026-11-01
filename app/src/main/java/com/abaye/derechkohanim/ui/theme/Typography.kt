package com.abaye.derechkohanim.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.abaye.derechkohanim.R

/**
 * Noto Sans Hebrew is the product font of the Tickta Civic design language, matching the web
 * client. It is bundled as a variable font (res/font/noto_sans_hebrew.ttf, weight axis) so it
 * works fully offline, with no downloadable or GMS font at runtime. Latin glyphs it lacks fall
 * back to the system sans.
 *
 * Civic weights: regular for body and big numbers (size carries a number, not bold), medium for
 * titles, labels and buttons, semibold only for the one emphasized line of a notice. There is
 * no bold in the system.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun noto(weight: FontWeight, axisWeight: Int) = Font(
    resId = R.font.noto_sans_hebrew,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(axisWeight)),
)

private val DkFontFamily = FontFamily(
    noto(FontWeight.Normal, 400),
    noto(FontWeight.Medium, 500),
    noto(FontWeight.SemiBold, 600),
)

private fun style(size: TextUnit, weight: FontWeight, lineHeight: TextUnit = TextUnit.Unspecified) =
    TextStyle(
        fontFamily = DkFontFamily,
        fontSize = size,
        fontWeight = weight,
        lineHeight = lineHeight,
    )

/** Civic type scale (tokens.json "type"): display 36, figure 32, stat 24, section 21, lead 18, body 16, label/small 14. */
internal val DkTypography = Typography(
    displayLarge = style(36.sp, FontWeight.Medium, 44.sp),
    displayMedium = style(32.sp, FontWeight.Normal, 40.sp),
    displaySmall = style(24.sp, FontWeight.Normal, 32.sp),
    headlineLarge = style(24.sp, FontWeight.Medium, 32.sp),
    headlineMedium = style(21.sp, FontWeight.Medium, 28.sp),
    headlineSmall = style(18.sp, FontWeight.Medium, 26.sp),
    titleLarge = style(21.sp, FontWeight.Medium, 28.sp),
    titleMedium = style(17.sp, FontWeight.Medium, 24.sp),
    titleSmall = style(16.sp, FontWeight.Medium, 24.sp),
    bodyLarge = style(16.sp, FontWeight.Normal, 24.sp),
    bodyMedium = style(15.sp, FontWeight.Normal, 22.sp),
    bodySmall = style(14.sp, FontWeight.Normal, 20.sp),
    labelLarge = style(16.sp, FontWeight.Medium, 24.sp),
    labelMedium = style(14.sp, FontWeight.Medium, 20.sp),
    labelSmall = style(12.sp, FontWeight.Medium, 16.sp),
)

/**
 * Driving screen only. Large enough to read in a glance, per spec 17.2. The distance keeps
 * medium weight (a deliberate step above Civic's regular figures) for legibility at speed.
 */
object DkNavTextStyles {
    val distance = style(FontSize.navPrimary, FontWeight.Medium)
    val instruction = style(21.sp, FontWeight.Medium, 28.sp)
    val street = style(18.sp, FontWeight.Normal, 26.sp)
    val stat = style(24.sp, FontWeight.Normal, 32.sp)
}
