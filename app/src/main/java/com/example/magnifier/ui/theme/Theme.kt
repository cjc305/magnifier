package com.example.magnifier.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

// CompositionLocal carrying the active ThemePalette. Composables that need
// non-M3 tokens (e.g. shadow tint) read from this. M3-mapped tokens
// (primary, surface, etc.) should be read from MaterialTheme.colorScheme.
val LocalThemePalette = staticCompositionLocalOf { ThemeSpec.Default.palette }

private fun ThemePalette.toColorScheme(isDark: Boolean) =
    if (isDark) {
        darkColorScheme(
            primary              = primary,
            onPrimary            = onPrimary,
            primaryContainer     = primaryContainer,
            onPrimaryContainer   = onPrimaryContainer,
            secondary            = secondary,
            onSecondary          = onSecondary,
            secondaryContainer   = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary             = secondary,
            background           = bg,
            onBackground         = onSurface,
            surface              = surface,
            onSurface            = onSurface,
            surfaceVariant       = surfaceContainer,
            onSurfaceVariant     = onSurfaceMuted,
            surfaceContainer     = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            outline              = outline,
            outlineVariant       = outlineVariant,
            error                = error,
            onError              = onError,
            errorContainer       = errorContainer,
            onErrorContainer     = onErrorContainer,
            scrim                = scrim,
        )
    } else {
        lightColorScheme(
            primary              = primary,
            onPrimary            = onPrimary,
            primaryContainer     = primaryContainer,
            onPrimaryContainer   = onPrimaryContainer,
            secondary            = secondary,
            onSecondary          = onSecondary,
            secondaryContainer   = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary             = secondary,
            background           = bg,
            onBackground         = onSurface,
            surface              = surface,
            onSurface            = onSurface,
            surfaceVariant       = surfaceContainer,
            onSurfaceVariant     = onSurfaceMuted,
            surfaceContainer     = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            outline              = outline,
            outlineVariant       = outlineVariant,
            error                = error,
            onError              = onError,
            errorContainer       = errorContainer,
            onErrorContainer     = onErrorContainer,
            scrim                = scrim,
        )
    }

@Composable
fun MagnifierTheme(
    spec: ThemeSpec = ThemeSpec.Default,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(spec) { spec.palette.toColorScheme(spec.isDark) }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalThemePalette provides spec.palette,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = MagnifierTypography,
            shapes      = MagnifierShapes,
            content     = content,
        )
    }
}
