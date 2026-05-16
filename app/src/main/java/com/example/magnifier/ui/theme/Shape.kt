package com.example.magnifier.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Radius scale — translated from mreminder-themes.json `radius` block.
// Soft-corner aesthetic without being bubbly. Apple-ish but not Cupertino.

val MagnifierShapes = Shapes(
    // Material 3 ladder:
    //   extraSmall → small chip / icon button bg
    //   small      → input fields
    //   medium     → buttons
    //   large      → cards
    //   extraLarge → modal / sheet
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),   // input
    medium     = RoundedCornerShape(12.dp),   // button
    large      = RoundedCornerShape(22.dp),   // card
    extraLarge = RoundedCornerShape(28.dp),   // bottom sheet / modal
)

// Convenience shape constants for non-M3-mapped surfaces
object MagnifierRadius {
    val chip = RoundedCornerShape(14.dp)
    val full = RoundedCornerShape(9999.dp)
}
