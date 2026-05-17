package com.example.magnifier.ui.theme

// Combines a palette with display metadata. The 6 specs mirror the
// mreminder shared design tokens (3 dark + 3 light).
//
// id is the persistence key — DO NOT change once shipped or users'
// saved preference would silently reset to Default.

data class ThemeSpec(
    val id: String,
    val displayName: String,
    val sublabel: String,
    val isDark: Boolean,
    val palette: ThemePalette,
) {
    companion object {
        val Noir     = ThemeSpec("noir",     "Noir",     "Amber",        isDark = true,  palette = NoirPalette)
        val Forest   = ThemeSpec("forest",   "Forest",   "Emerald",      isDark = true,  palette = ForestPalette)
        val Cyber    = ThemeSpec("cyber",    "Cyber",    "Teal",         isDark = true,  palette = CyberPalette)
        val Blush    = ThemeSpec("blush",    "Blush",    "Warm Studio",  isDark = false, palette = BlushPalette)
        val Ice      = ThemeSpec("ice",      "Ice",      "Frosted",      isDark = false, palette = IcePalette)
        val Lavender = ThemeSpec("lavender", "Lavender", "Mist",         isDark = false, palette = LavenderPalette)

        val All = listOf(Noir, Forest, Cyber, Blush, Ice, Lavender)
        val Default = Noir

        fun fromId(id: String): ThemeSpec = All.firstOrNull { it.id == id } ?: Default
    }
}
