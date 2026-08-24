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

private val YonteLightColors = lightColorScheme(
    primary = Color(0xFF4141D8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E4FF),
    onPrimaryContainer = Color(0xFF17164E),
    secondary = Color(0xFF7465D2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECE8FF),
    onSecondaryContainer = Color(0xFF211A4A),
    tertiary = Color(0xFFD66F54),
    onTertiary = Color.White,
    background = Color(0xFFF8F7F3),
    onBackground = Color(0xFF20201E),
    surface = Color(0xFFFFFEFB),
    onSurface = Color(0xFF20201E),
    surfaceVariant = Color(0xFFEAE8E1),
    onSurfaceVariant = Color(0xFF66645E),
    outline = Color(0xFFB9B6AE),
)

private val YonteDarkColors = darkColorScheme(
    primary = Color(0xFFBDBBFF),
    onPrimary = Color(0xFF20206F),
    primaryContainer = Color(0xFF303096),
    onPrimaryContainer = Color(0xFFE5E4FF),
    secondary = Color(0xFFC9BEFF),
    onSecondary = Color(0xFF34276E),
    secondaryContainer = Color(0xFF4C3D86),
    onSecondaryContainer = Color(0xFFECE8FF),
    tertiary = Color(0xFFFFB59F),
    onTertiary = Color(0xFF5A190A),
    background = Color(0xFF111110),
    onBackground = Color(0xFFE8E6E0),
    surface = Color(0xFF191918),
    onSurface = Color(0xFFE8E6E0),
    surfaceVariant = Color(0xFF2B2B28),
    onSurfaceVariant = Color(0xFFC2C0B8),
    outline = Color(0xFF77756E),
)

private val YonteShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private val YonteTypography = Typography(
    displaySmall = androidx.compose.material3.Typography().displaySmall.copy(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
    headlineSmall = androidx.compose.material3.Typography().headlineSmall.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    titleLarge = androidx.compose.material3.Typography().titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = androidx.compose.material3.Typography().titleMedium.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = androidx.compose.material3.Typography().bodyLarge.copy(fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = androidx.compose.material3.Typography().bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = androidx.compose.material3.Typography().labelLarge.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
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
