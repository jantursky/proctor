package com.jantursky.proctor.sample.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** The colours of design/tokens.css that Material has no slot for. */
@Immutable
class ProctorColors(
    val canvas: Color,
    val band: Color,
    val hairline: Color,
    val ink: Color,
    val onInk: Color,
    val body: Color,
    val caption: Color,
    val link: Color,
    val pass: Color,
    val fail: Color,
    val notGraded: Color,
)

private val Light =
    ProctorColors(
        canvas = Color(0xFFFFFFFF),
        band = Color(0xFFF3F4F6),
        hairline = Color(0xFFE5E7EB),
        ink = Color(0xFF000000),
        onInk = Color(0xFFFFFFFF),
        body = Color(0xFF4A5565),
        caption = Color(0xFF6A7282),
        link = Color(0xFF155DFC),
        pass = Color(0xFF16794C),
        fail = Color(0xFFB42318),
        notGraded = Color(0xFFA15C07),
    )

private val Dark =
    ProctorColors(
        canvas = Color(0xFF0B0F19),
        band = Color(0xFF101828),
        hairline = Color(0xFF2C3344),
        ink = Color(0xFFF9FAFB),
        onInk = Color(0xFF0B0F19),
        body = Color(0xFFD1D5DC),
        caption = Color(0xFF99A1AF),
        link = Color(0xFF51A2FF),
        pass = Color(0xFF4ADE80),
        fail = Color(0xFFF87171),
        notGraded = Color(0xFFFBBF24),
    )

val LocalProctorColors = staticCompositionLocalOf { Light }

val ProctorMono = FontFamily.Monospace

private val Type =
    Typography(
        headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 32.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
        labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        labelSmall =
            TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.4.sp,
            ),
    )

@Composable
fun ProctorTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val c = if (dark) Dark else Light
    val scheme =
        (if (dark) darkColorScheme() else lightColorScheme()).copy(
            primary = c.ink,
            onPrimary = c.onInk,
            background = c.canvas,
            onBackground = c.ink,
            surface = c.canvas,
            onSurface = c.ink,
            surfaceVariant = c.band,
            onSurfaceVariant = c.body,
            outline = c.hairline,
            outlineVariant = c.hairline,
            secondaryContainer = c.band,
            onSecondaryContainer = c.ink,
        )
    CompositionLocalProvider(LocalProctorColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = Type, content = content)
    }
}

val proctorColors: ProctorColors
    @Composable get() = LocalProctorColors.current
