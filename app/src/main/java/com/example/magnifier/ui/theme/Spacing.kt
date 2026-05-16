package com.example.magnifier.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Spacing scale — translated from mreminder-themes.json `spacing` block.
// All paddings, gaps, and offsets MUST go through this. No raw .dp literals
// in feature screens unless there's a documented exception.

data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
)

val LocalSpacing = compositionLocalOf { Spacing() }
