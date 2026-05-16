package com.example.magnifier.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// v1 ships with the Noir / Amber palette only — single dark theme that
// pairs with the amber launcher icon for end-to-end brand cohesion and
// matches the dark-camera-UI convention (Apple Camera, Google Camera).
//
// Dynamic Color is intentionally disabled: on Android 12+ it would let the
// user's wallpaper override our Amber primary, breaking the brand link
// between launcher icon and in-app UI.
//
// v1.1 may add a light variant for bright-environment accessibility.

private val NoirColorScheme = darkColorScheme(
    primary             = NoirPalette.Primary,
    onPrimary           = NoirPalette.OnPrimary,
    primaryContainer    = NoirPalette.PrimaryContainer,
    onPrimaryContainer  = NoirPalette.OnPrimaryContainer,
    secondary           = NoirPalette.Secondary,
    onSecondary         = NoirPalette.OnSecondary,
    secondaryContainer  = NoirPalette.SecondaryContainer,
    onSecondaryContainer = NoirPalette.OnSecondaryContainer,
    tertiary            = NoirPalette.Secondary,
    background          = NoirPalette.Bg,
    onBackground        = NoirPalette.OnSurface,
    surface             = NoirPalette.Surface,
    onSurface           = NoirPalette.OnSurface,
    surfaceVariant      = NoirPalette.SurfaceContainer,
    onSurfaceVariant    = NoirPalette.OnSurfaceMuted,
    surfaceContainer    = NoirPalette.SurfaceContainer,
    surfaceContainerHigh = NoirPalette.SurfaceContainerHigh,
    outline             = NoirPalette.Outline,
    outlineVariant      = NoirPalette.OutlineVariant,
    error               = NoirPalette.Error,
    onError             = NoirPalette.OnError,
    errorContainer      = NoirPalette.ErrorContainer,
    onErrorContainer    = NoirPalette.OnErrorContainer,
    scrim               = NoirPalette.Scrim,
)

@Composable
fun MagnifierTheme(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = NoirColorScheme,
            typography  = MagnifierTypography,
            shapes      = MagnifierShapes,
            content     = content,
        )
    }
}
