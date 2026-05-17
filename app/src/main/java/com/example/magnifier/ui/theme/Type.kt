@file:OptIn(ExperimentalTextApi::class)

package com.example.magnifier.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.magnifier.R

// Space Grotesk — bundled as a single variable TTF (~134 KB) under OFL.
// License text mirrored in repo `licenses/OFL-SpaceGrotesk.txt`.
//
// FontVariation.weight requires API 26+ to actually interpolate the wght
// axis. On API 24-25 the system renders the font's default weight from
// the file (~0.3% of Play installs — acceptable fallback).
//
// CJK characters are not in the Space Grotesk glyph set; Compose falls
// back to the system CJK font (Noto Sans CJK on most Android), which is
// the correct behavior — keeps zh-TW labels native while Latin numerics
// and ALL CAPS labels pick up the geometric Space Grotesk feel.

private val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Light,    variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(R.font.space_grotesk, FontWeight.Normal,   variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.space_grotesk, FontWeight.Medium,   variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.space_grotesk, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.space_grotesk, FontWeight.Bold,     variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

val MagnifierTypography = Typography(
    displayLarge   = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Light,    fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-3).sp),
    displayMedium  = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Light,    fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = (-2).sp),
    displaySmall   = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Light,    fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-1.5).sp),
    headlineLarge  = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold,     fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold,     fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.6).sp),
    headlineSmall  = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp),
    titleLarge     = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    titleMedium    = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = (-0.1).sp),
    titleSmall     = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    bodyLarge      = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
    bodyMedium     = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    bodySmall      = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal,   fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp),
    labelLarge     = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.5.sp),
    labelMedium    = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.8.sp),
    labelSmall     = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 1.0.sp),
)

@Deprecated("Use MagnifierTypography", ReplaceWith("MagnifierTypography"))
val Typography = MagnifierTypography
