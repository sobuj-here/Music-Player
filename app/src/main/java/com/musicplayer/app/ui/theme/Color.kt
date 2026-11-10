package com.musicplayer.app.ui.theme

import androidx.compose.ui.graphics.Color

// Base Dark Colors
val BackgroundDark = Color(0xFF101216)
val SurfaceDark = Color(0xFF191B22)
val SurfaceVariantDark = Color(0xFF232630)
val TextPrimaryDark = Color(0xFFF0F2F8)
val TextSecondaryDark = Color(0xFF989EAE)
val TextTertiaryDark = Color(0xFF656B7D)
val DividerDark = Color(0xFF282C38)

// Base Light Colors
val BackgroundLight = Color(0xFFF7F8FC)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFEDEFF6)
val TextPrimaryLight = Color(0xFF14161C)
val TextSecondaryLight = Color(0xFF555B6D)
val TextTertiaryLight = Color(0xFF8B92A4)
val DividerLight = Color(0xFFE2E5F0)

// Accent Colors
data class AccentPalette(
    val name: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

val AccentPalettes = listOf(
    AccentPalette(
        name = "Cyan Neon",
        primary = Color(0xFF00E5FF),
        secondary = Color(0xFF00B0FF),
        tertiary = Color(0xFF1DE9B6)
    ),
    AccentPalette(
        name = "Deep Purple",
        primary = Color(0xFFB388FF),
        secondary = Color(0xFF7C4DFF),
        tertiary = Color(0xFFEA80FC)
    ),
    AccentPalette(
        name = "Electric Blue",
        primary = Color(0xFF448AFF),
        secondary = Color(0xFF2979FF),
        tertiary = Color(0xFF82B1FF)
    ),
    AccentPalette(
        name = "Sunset Orange",
        primary = Color(0xFFFF6E40),
        secondary = Color(0xFFFF3D00),
        tertiary = Color(0xFFFF9E80)
    ),
    AccentPalette(
        name = "Emerald Green",
        primary = Color(0xFF00E676),
        secondary = Color(0xFF00C853),
        tertiary = Color(0xFF69F0AE)
    )
)
