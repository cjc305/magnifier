package com.example.magnifier.ui.theme

import androidx.compose.ui.graphics.Color

// Noir / Amber palette — translated from shared/design-tokens/mreminder-themes.json
// Atmospheric color rules:
//   - No pure black (#000000) — backgrounds carry warm amber tint
//   - No pure white (#FFFFFF) — onSurface is warm cream (#FFF8EC)
//   - Layered surfaces step up tonal warmth, not just lightness
//   - Shadow color is amber (#D97706), not gray — premium glow on elevation
//
// Default theme for the magnifier app — pairs with the amber launcher icon
// (#FFD54F → #FF8F00 gradient) for end-to-end brand cohesion.

object NoirPalette {
    // Background tier — amber-tinted near-black
    val Bg = Color(0xFF0F0901)
    val Surface = Color(0xFF1C1202)
    val SurfaceContainer = Color(0xFF2A1A05)
    val SurfaceContainerHigh = Color(0xFF38220A)

    // Content tier — warm cream, not sterile white
    val OnSurface = Color(0xFFFFF8EC)
    val OnSurfaceMuted = Color(0xFFE8A030)

    // Strokes / dividers
    val Outline = Color(0xFF3A2210)
    val OutlineVariant = Color(0xFF2E1A0A)

    // Primary accent — amber (matches launcher icon)
    val Primary = Color(0xFFFBBF24)
    val OnPrimary = Color(0xFF3A1800)
    val PrimaryContainer = Color(0xFF5A2C00)
    val OnPrimaryContainer = Color(0xFFFFE090)

    // Secondary accent — cool cyan, contrasts amber without being neutral
    val Secondary = Color(0xFF22D3EE)
    val OnSecondary = Color(0xFF003040)
    val SecondaryContainer = Color(0xFF004D66)
    val OnSecondaryContainer = Color(0xFFB0F0FF)

    // Destructive — warm red, not pure red, to keep palette warmth
    val Error = Color(0xFFFF6B5C)
    val OnError = Color(0xFF3A0500)
    val ErrorContainer = Color(0xFF5A1800)
    val OnErrorContainer = Color(0xFFFFD4CC)

    // Shadow — amber glow on elevation, not gray drop-shadow
    val Shadow = Color(0xFFD97706)

    // Scrim — for modal overlays. Tinted black with alpha, never pure 000.
    // Use this for ImageViewer backdrop instead of Color.Black.
    val Scrim = Color(0xCC0A0600)  // ~80% alpha, warm-tinted

    // Camera control bar overlay — semi-opaque amber-tinted for legibility
    // over varying camera content. Use as gradient end-stop or solid alpha.
    val ControlBarTop = Color(0x00120800)       // fully transparent at top
    val ControlBarBottom = Color(0xE6120800)    // ~90% alpha at bottom
}
