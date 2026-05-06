package com.example.mytodo.desktop.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val BrandIndigo = Color(0xFF5B4BFF)
val BrandMagenta = Color(0xFFFF3D9A)
val BrandAmber = Color(0xFFFFB300)
val BrandCoral = Color(0xFFFF6B6B)

val SurfaceLight = Color(0xFFFFFCFD)
val SurfaceVariantLight = Color(0xFFF1EEFF)
val OnSurfaceLight = Color(0xFF1A1530)
val OnSurfaceVariantLight = Color(0xFF6A6585)
val OutlineLight = Color(0xFFCFC8E5)

val SurfaceDark = Color(0xFF15101F)
val SurfaceVariantDark = Color(0xFF221B36)
val OnSurfaceDark = Color(0xFFEFE9FF)
val OnSurfaceVariantDark = Color(0xFFB0A6CF)
val OutlineDark = Color(0xFF4A4262)

val BrandGradient = Brush.linearGradient(colors = listOf(BrandIndigo, BrandMagenta))
val BrandGradientReversed = Brush.linearGradient(colors = listOf(BrandMagenta, BrandIndigo))
