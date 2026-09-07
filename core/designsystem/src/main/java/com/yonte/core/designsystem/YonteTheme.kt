package com.yonte.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Mineral neutrals and restrained gold keep the identity consistent across languages.
// Light surfaces use bronze accents; dark surfaces pair gold actions with dark text.
private val YonteLightColors = lightColorScheme(
    primary = Color(0xFF766034),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEE1C6),
    onPrimaryContainer = Color(0xFF302715),
    inversePrimary = Color(0xFFC6AA70),
    secondary = Color(0xFF355E59),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEAE5),
    onSecondaryContainer = Color(0xFF173C37),
    tertiary = Color(0xFF766034),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF1E1BF),
    onTertiaryContainer = Color(0xFF302715),
    background = Color(0xFFF3EFE5),
    onBackground = Color(0xFF101918),
    surface = Color(0xFFFCFAF4),
    onSurface = Color(0xFF101918),
    surfaceVariant = Color(0xFFE8E5DA),
    onSurfaceVariant = Color(0xFF515B55),
    surfaceTint = Color(0xFFC6AA70),
    inverseSurface = Color(0xFF26332F),
    inverseOnSurface = Color(0xFFF3EFE5),
    outline = Color(0xFF778078),
    outlineVariant = Color(0xFFD0D3C7),
    surfaceDim = Color(0xFFE2DED4),
    surfaceBright = Color(0xFFFCFAF4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F5ED),
    surfaceContainer = Color(0xFFF0EDE3),
    surfaceContainerHigh = Color(0xFFEAE7DD),
    surfaceContainerHighest = Color(0xFFE4E1D7),
)

private val YonteDarkColors = darkColorScheme(
    primary = Color(0xFFC6AA70),
    onPrimary = Color(0xFF101918),
    primaryContainer = Color(0xFF443C29),
    onPrimaryContainer = Color(0xFFF0E0BD),
    inversePrimary = Color(0xFF766034),
    secondary = Color(0xFFA3CDC0),
    onSecondary = Color(0xFF103A30),
    secondaryContainer = Color(0xFF294C43),
    onSecondaryContainer = Color(0xFFDCEAE5),
    tertiary = Color(0xFFDDC590),
    onTertiary = Color(0xFF3C2E12),
    tertiaryContainer = Color(0xFF564423),
    onTertiaryContainer = Color(0xFFF1E1BF),
    background = Color(0xFF101918),
    onBackground = Color(0xFFF3EFE5),
    surface = Color(0xFF15201E),
    onSurface = Color(0xFFF3EFE5),
    surfaceVariant = Color(0xFF2E3A35),
    onSurfaceVariant = Color(0xFFC1CABF),
    surfaceTint = Color(0xFFC6AA70),
    inverseSurface = Color(0xFFE5E8DE),
    inverseOnSurface = Color(0xFF26332F),
    outline = Color(0xFF8A958C),
    outlineVariant = Color(0xFF414D45),
    surfaceDim = Color(0xFF101918),
    surfaceBright = Color(0xFF36413C),
    surfaceContainerLowest = Color(0xFF0C1412),
    surfaceContainerLow = Color(0xFF18231F),
    surfaceContainer = Color(0xFF1C2823),
    surfaceContainerHigh = Color(0xFF26322D),
    surfaceContainerHighest = Color(0xFF303D36),
)

private val YonteShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val BaseTypography = Typography()

// Default platform fonts provide Arabic shaping and language-aware font fallback.
private val YonteTypography = Typography(
    displaySmall = BaseTypography.displaySmall.copy(fontSize = 36.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
    headlineSmall = BaseTypography.headlineSmall.copy(fontSize = 28.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
    titleLarge = BaseTypography.titleLarge.copy(fontSize = 22.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
    titleMedium = BaseTypography.titleMedium.copy(fontSize = 16.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    bodyLarge = BaseTypography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    bodyMedium = BaseTypography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
    labelLarge = BaseTypography.labelLarge.copy(fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
)

@Composable
fun YonteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        darkTheme -> YonteDarkColors
        else -> YonteLightColors
    }
    MaterialTheme(colorScheme = colors, typography = YonteTypography, shapes = YonteShapes, content = content)
}
