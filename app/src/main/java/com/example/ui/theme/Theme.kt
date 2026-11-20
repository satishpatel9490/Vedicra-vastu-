package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.AppThemeMode

@Immutable
data class VedicraBrandColors(
    val goldAccent: Color,
    val goldContainer: Color,
    val onGoldContainer: Color,
    val actionDarkGreen: Color,
    val onActionDarkGreen: Color,
    val cardBorder: Color,
    val headerGradientStart: Color,
    val headerGradientEnd: Color,
    val isDark: Boolean
)

val LocalVedicraColors = staticCompositionLocalOf {
    VedicraBrandColors(
        goldAccent = MutedGold,
        goldContainer = SoftGoldContainer,
        onGoldContainer = OnGoldContainer,
        actionDarkGreen = ForestGreen,
        onActionDarkGreen = Color.White,
        cardBorder = SoftOutline,
        headerGradientStart = ForestGreen,
        headerGradientEnd = ForestGreenDark,
        isDark = false
    )
}

private val VedicraShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = ForestGreenContainer,
    onPrimaryContainer = OnForestGreenContainer,
    secondary = MutedGold,
    onSecondary = Color.White,
    secondaryContainer = SoftGoldContainer,
    onSecondaryContainer = OnGoldContainer,
    tertiary = EarthyBronze,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF141D19),
    surface = LightSurface,
    onSurface = Color(0xFF141D19),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF424E47),
    outline = SoftOutline,
    outlineVariant = Color(0xFFE3E8E4),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = Color(0xFF410E0B)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkSagePrimary,
    onPrimary = Color(0xFF0A281E),
    primaryContainer = Color(0xFF1F4738),
    onPrimaryContainer = Color(0xFFCDEEE0),
    secondary = DarkGoldAccent,
    onSecondary = Color(0xFF2B1E02),
    secondaryContainer = Color(0xFF4B380B),
    onSecondaryContainer = Color(0xFFF9E6B8),
    tertiary = Color(0xFFD2B48C),
    onTertiary = Color(0xFF2B1E0E),
    background = DarkBackground,
    onBackground = Color(0xFFE9EFEA),
    surface = DarkSurface,
    onSurface = Color(0xFFE9EFEA),
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = Color(0xFFB5C4BC),
    outline = DarkOutline,
    outlineVariant = Color(0xFF283630),
    error = ErrorRedDark,
    onError = Color(0xFF601410),
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorContainerLight
)

private val SageGreenColorScheme = lightColorScheme(
    primary = SagePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCEE3D6),
    onPrimaryContainer = Color(0xFF0E291C),
    secondary = MutedGold,
    onSecondary = Color.White,
    secondaryContainer = SoftGoldContainer,
    onSecondaryContainer = OnGoldContainer,
    tertiary = EarthyBronze,
    onTertiary = Color.White,
    background = SageSurface,
    onBackground = Color(0xFF142119),
    surface = SageCard,
    onSurface = Color(0xFF142119),
    surfaceVariant = SageSurfaceVariant,
    onSurfaceVariant = Color(0xFF3F5246),
    outline = Color(0xFFC2D1C6),
    outlineVariant = Color(0xFFDCE6DF),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = Color(0xFF410E0B)
)

private val IvoryGoldColorScheme = lightColorScheme(
    primary = Color(0xFF1D4434),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFECE3),
    onPrimaryContainer = Color(0xFF0D281D),
    secondary = BurnishedGold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6E7C3),
    onSecondaryContainer = Color(0xFF382604),
    tertiary = EarthyBronze,
    onTertiary = Color.White,
    background = IvoryBackground,
    onBackground = Color(0xFF231D15),
    surface = IvorySurface,
    onSurface = Color(0xFF231D15),
    surfaceVariant = IvorySurfaceVariant,
    onSurfaceVariant = Color(0xFF5A4E3C),
    outline = EarthyOutline,
    outlineVariant = Color(0xFFEBE1CE),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = Color(0xFF410E0B)
)

@Composable
fun VedicraVastuTheme(
    themeMode: AppThemeMode = AppThemeMode.IVORY_GOLD,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.SAGE_GREEN -> SageGreenColorScheme
        AppThemeMode.IVORY_GOLD -> IvoryGoldColorScheme
    }

    val brandColors = when (themeMode) {
        AppThemeMode.LIGHT -> VedicraBrandColors(
            goldAccent = MutedGold,
            goldContainer = SoftGoldContainer,
            onGoldContainer = OnGoldContainer,
            actionDarkGreen = ForestGreen,
            onActionDarkGreen = Color.White,
            cardBorder = SoftOutline,
            headerGradientStart = ForestGreen,
            headerGradientEnd = ForestGreenDark,
            isDark = false
        )
        AppThemeMode.DARK -> VedicraBrandColors(
            goldAccent = DarkGoldAccent,
            goldContainer = Color(0xFF45340B),
            onGoldContainer = Color(0xFFF9E6B8),
            actionDarkGreen = Color(0xFF24664F),
            onActionDarkGreen = Color.White,
            cardBorder = DarkOutline,
            headerGradientStart = Color(0xFF1B3A2E),
            headerGradientEnd = Color(0xFF10241D),
            isDark = true
        )
        AppThemeMode.SAGE_GREEN -> VedicraBrandColors(
            goldAccent = MutedGold,
            goldContainer = SoftGoldContainer,
            onGoldContainer = OnGoldContainer,
            actionDarkGreen = SagePrimary,
            onActionDarkGreen = Color.White,
            cardBorder = Color(0xFFC2D1C6),
            headerGradientStart = Color(0xFF285341),
            headerGradientEnd = Color(0xFF19382B),
            isDark = false
        )
        AppThemeMode.IVORY_GOLD -> VedicraBrandColors(
            goldAccent = BurnishedGold,
            goldContainer = Color(0xFFF5E5C0),
            onGoldContainer = Color(0xFF382604),
            actionDarkGreen = Color(0xFF1D4434),
            onActionDarkGreen = Color.White,
            cardBorder = EarthyOutline,
            headerGradientStart = Color(0xFF1D4434),
            headerGradientEnd = Color(0xFF2E3A26),
            isDark = false
        )
    }

    CompositionLocalProvider(LocalVedicraColors provides brandColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VedicraTypography,
            shapes = VedicraShapes,
            content = content
        )
    }
}
