package com.abaye.derechkohanim.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.abaye.derechkohanim.R

/**
 * Rubik is the product font, matching the web client. It is bundled as a variable font
 * (res/font/rubik.ttf, weight axis) so it works fully offline, with no downloadable or GMS
 * font at runtime. Rubik covers both Hebrew and Latin, so one family serves both languages.
 *
 * The generated tokens carry sizes only; the family lives here and is applied through the
 * theme, never per composable.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun rubik(weight: FontWeight, axisWeight: Int) = Font(
    resId = R.font.rubik,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(axisWeight)),
)

private val DkFontFamily = FontFamily(
    rubik(FontWeight.Normal, 400),
    rubik(FontWeight.Medium, 500),
    rubik(FontWeight.SemiBold, 600),
    rubik(FontWeight.Bold, 700),
)

private fun style(size: androidx.compose.ui.unit.TextUnit, weight: FontWeight) = TextStyle(
    fontFamily = DkFontFamily,
    fontSize = size,
    fontWeight = weight,
)

internal val DkTypography = Typography(
    displayLarge = style(FontSize.display, FontWeight.Bold),
    displayMedium = style(FontSize.heading, FontWeight.Bold),
    displaySmall = style(FontSize.title, FontWeight.SemiBold),
    headlineLarge = style(FontSize.heading, FontWeight.SemiBold),
    headlineMedium = style(FontSize.title, FontWeight.SemiBold),
    headlineSmall = style(FontSize.bodyLarge, FontWeight.SemiBold),
    titleLarge = style(FontSize.title, FontWeight.SemiBold),
    titleMedium = style(FontSize.bodyLarge, FontWeight.Medium),
    titleSmall = style(FontSize.body, FontWeight.Medium),
    bodyLarge = style(FontSize.bodyLarge, FontWeight.Normal),
    bodyMedium = style(FontSize.body, FontWeight.Normal),
    bodySmall = style(FontSize.label, FontWeight.Normal),
    labelLarge = style(FontSize.body, FontWeight.Medium),
    labelMedium = style(FontSize.label, FontWeight.Medium),
    labelSmall = style(FontSize.caption, FontWeight.Medium),
)

/**
 * Driving screen only. Large enough to read in a glance, per spec 17.2.
 */
object DkNavTextStyles {
    val distance = style(FontSize.navPrimary, FontWeight.Bold)
    val instruction = style(FontSize.heading, FontWeight.SemiBold)
    val street = style(FontSize.title, FontWeight.Medium)
    val stat = style(FontSize.bodyLarge, FontWeight.SemiBold)
}
