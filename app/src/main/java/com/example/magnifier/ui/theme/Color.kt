package com.example.magnifier.ui.theme

import androidx.compose.ui.graphics.Color

// Palette data class — single shape used by all 6 themes.
// Tokens beyond M3 ColorScheme:
//   - shadow:   amber/teal/etc. tint used for elevation glow (vs gray drop-shadow)
//   - scrim:    warm-tinted near-black for modal overlays (never pure #000000)
//
// All values translated from shared/design-tokens/mreminder-themes.json.

data class ThemePalette(
    val bg: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val outline: Color,
    val outlineVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val shadow: Color,
    val scrim: Color,
    val error: Color = Color(0xFFFF6B5C),
    val onError: Color = Color(0xFF3A0500),
    val errorContainer: Color = Color(0xFF5A1800),
    val onErrorContainer: Color = Color(0xFFFFD4CC),
)

// ── Dark themes ─────────────────────────────────────────────

val NoirPalette = ThemePalette(
    bg                   = Color(0xFF0F0901),
    surface              = Color(0xFF1C1202),
    surfaceContainer     = Color(0xFF2A1A05),
    surfaceContainerHigh = Color(0xFF38220A),
    onSurface            = Color(0xFFFFF8EC),
    onSurfaceMuted       = Color(0xFFE8A030),
    outline              = Color(0xFF3A2210),
    outlineVariant       = Color(0xFF2E1A0A),
    primary              = Color(0xFFFBBF24),
    onPrimary            = Color(0xFF3A1800),
    primaryContainer     = Color(0xFF5A2C00),
    onPrimaryContainer   = Color(0xFFFFE090),
    secondary            = Color(0xFF22D3EE),
    onSecondary          = Color(0xFF003040),
    secondaryContainer   = Color(0xFF004D66),
    onSecondaryContainer = Color(0xFFB0F0FF),
    shadow               = Color(0xFFD97706),
    scrim                = Color(0xCC0A0600),
)

val ForestPalette = ThemePalette(
    bg                   = Color(0xFF030F09),
    surface              = Color(0xFF0A1F12),
    surfaceContainer     = Color(0xFF122B1A),
    surfaceContainerHigh = Color(0xFF1A3824),
    onSurface            = Color(0xFFF0FFF6),
    onSurfaceMuted       = Color(0xFF6EC99A),
    outline              = Color(0xFF1A3824),
    outlineVariant       = Color(0xFF122B1A),
    primary              = Color(0xFF34D399),
    onPrimary            = Color(0xFF003320),
    primaryContainer     = Color(0xFF004D30),
    onPrimaryContainer   = Color(0xFFB8F5D8),
    secondary            = Color(0xFFFBBF24),
    onSecondary          = Color(0xFF3A1800),
    secondaryContainer   = Color(0xFF5A2C00),
    onSecondaryContainer = Color(0xFFFFE0A0),
    shadow               = Color(0xFF34D399),
    scrim                = Color(0xCC020A05),
)

val CyberPalette = ThemePalette(
    bg                   = Color(0xFF000D14),
    surface              = Color(0xFF03161F),
    surfaceContainer     = Color(0xFF0A2030),
    surfaceContainerHigh = Color(0xFF123040),
    onSurface            = Color(0xFFEEFBFF),
    onSurfaceMuted       = Color(0xFF4ABDE0),
    outline              = Color(0xFF162C40),
    outlineVariant       = Color(0xFF0E2232),
    primary              = Color(0xFF22D3EE),
    onPrimary            = Color(0xFF003040),
    primaryContainer     = Color(0xFF004D66),
    onPrimaryContainer   = Color(0xFFAADFF5),
    secondary            = Color(0xFF818CF8),
    onSecondary          = Color(0xFF1E1A4A),
    secondaryContainer   = Color(0xFF3730A3),
    onSecondaryContainer = Color(0xFFE0E7FF),
    shadow               = Color(0xFF22D3EE),
    scrim                = Color(0xCC00080E),
)

// ── Light themes ────────────────────────────────────────────

val BlushPalette = ThemePalette(
    bg                   = Color(0xFFFAE8E8),
    surface              = Color(0xFFFFF0F0),
    surfaceContainer     = Color(0xFFF0D4D4),
    surfaceContainerHigh = Color(0xFFE8C8C8),
    onSurface            = Color(0xFF1C0606),
    onSurfaceMuted       = Color(0xFF9A6868),
    outline              = Color(0xFFE0C0C0),
    outlineVariant       = Color(0xFFEDD0D0),
    primary              = Color(0xFFE31937),
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFFFFD6DC),
    onPrimaryContainer   = Color(0xFF6B0010),
    secondary            = Color(0xFF059669),
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF022C22),
    shadow               = Color(0xFFE31937),
    scrim                = Color(0xB31C0606),
)

val IcePalette = ThemePalette(
    bg                   = Color(0xFFE6F0FA),
    surface              = Color(0xFFF0F7FF),
    surfaceContainer     = Color(0xFFD4E8F8),
    surfaceContainerHigh = Color(0xFFC4DCEF),
    onSurface            = Color(0xFF00071A),
    onSurfaceMuted       = Color(0xFF3A5E80),
    outline              = Color(0xFFC0D8F0),
    outlineVariant       = Color(0xFFD4E8FF),
    primary              = Color(0xFF007AFF),
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFFCCE4FF),
    onPrimaryContainer   = Color(0xFF003480),
    secondary            = Color(0xFFD97706),
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFFFF3C4),
    onSecondaryContainer = Color(0xFF5A3000),
    shadow               = Color(0xFF007AFF),
    scrim                = Color(0xB300071A),
)

val LavenderPalette = ThemePalette(
    bg                   = Color(0xFFEDE5FF),
    surface              = Color(0xFFF5EEFF),
    surfaceContainer     = Color(0xFFE0D4FF),
    surfaceContainerHigh = Color(0xFFD0C0F8),
    onSurface            = Color(0xFF130A28),
    onSurfaceMuted       = Color(0xFF5A4470),
    outline              = Color(0xFFD0B8F0),
    outlineVariant       = Color(0xFFE0CCF8),
    primary              = Color(0xFF6750A4),
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFFEADDFF),
    onPrimaryContainer   = Color(0xFF2B0078),
    secondary            = Color(0xFF059669),
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF022C22),
    shadow               = Color(0xFF6750A4),
    scrim                = Color(0xB3130A28),
)
