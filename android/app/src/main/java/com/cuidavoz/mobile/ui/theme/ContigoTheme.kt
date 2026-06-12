package com.cuidavoz.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

// Base Colors
val PrimaryColor = Color(0xFF0F6B6E)
val BackgroundColor = Color(0xFFFBF7EF)
val SurfaceColor = Color(0xFFFFFFFF)
val OnSurfaceColor = Color(0xFF14213D)
val SecondaryColor = Color(0xFFE7F3F1)

// Patient Mode Specialized Colors
val PatientBackground = Color(0xFFFBF7EC)
val StatusBackground = Color(0xFFE9DDF8)
val StatusText = Color(0xFF0B1F3A)
val VoiceButtonBackground = Color(0xFFE9DDF8)
val MeasurePressureButton = Color(0xFFE3F5F2)
val HelpButton = Color(0xFFFDE8EA)
val BrandText = Color(0xFF0F7C78)

// Caregiver Mode Specialized Colors
val MedicationIcon = Color(0xFF4D8A4B)
val HistoryIcon = Color(0xFF0F6B6E)
val ReportIcon = Color(0xFF7A4BA3)
val ContactIcon = Color(0xFFC85A5A)
val SettingsIcon = Color(0xFF5E6E8A)
val BackupIcon = Color(0xFF19857B)

// Functional Colors
val SuccessGreen = Color(0xFF1E8E3E)
val ErrorRed = Color(0xFFD44E4E)
val InfoBlue = Color(0xFF2176D9)

@Immutable
data class ContigoExtraColors(
    val patientBackground: Color = Color.Unspecified,
    val statusBackground: Color = Color.Unspecified,
    val statusText: Color = Color.Unspecified,
    val voiceButtonBackground: Color = Color.Unspecified,
    val measurePressureButton: Color = Color.Unspecified,
    val helpButton: Color = Color.Unspecified,
    val brandText: Color = Color.Unspecified,
    val medicationIcon: Color = Color.Unspecified,
    val historyIcon: Color = Color.Unspecified,
    val reportIcon: Color = Color.Unspecified,
    val contactIcon: Color = Color.Unspecified,
    val settingsIcon: Color = Color.Unspecified,
    val backupIcon: Color = Color.Unspecified,
    val successGreen: Color = Color.Unspecified,
    val errorRed: Color = Color.Unspecified,
    val infoBlue: Color = Color.Unspecified,
)

val LocalExtraColors = staticCompositionLocalOf { ContigoExtraColors() }

private val LightExtraColors = ContigoExtraColors(
    patientBackground = PatientBackground,
    statusBackground = StatusBackground,
    statusText = StatusText,
    voiceButtonBackground = VoiceButtonBackground,
    measurePressureButton = MeasurePressureButton,
    helpButton = HelpButton,
    brandText = BrandText,
    medicationIcon = MedicationIcon,
    historyIcon = HistoryIcon,
    reportIcon = ReportIcon,
    contactIcon = ContactIcon,
    settingsIcon = SettingsIcon,
    backupIcon = BackupIcon,
    successGreen = SuccessGreen,
    errorRed = ErrorRed,
    infoBlue = InfoBlue,
)

private val LightColors = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    background = BackgroundColor,
    onBackground = OnSurfaceColor,
    surface = SurfaceColor,
    onSurface = OnSurfaceColor,
    secondary = SecondaryColor,
)

private val PatientTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
    ),
)

private val CaregiverTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
)

@Immutable
data class ContigoDimensions(
    val buttonMinHeight: androidx.compose.ui.unit.Dp = 72.dp,
    val cardPadding: androidx.compose.ui.unit.Dp = 20.dp,
    val headerHeight: androidx.compose.ui.unit.Dp = 56.dp,
    val iconSize: androidx.compose.ui.unit.Dp = 28.dp,
)

val LocalDimensions = staticCompositionLocalOf { ContigoDimensions() }

private val PatientDimensions = ContigoDimensions(
    buttonMinHeight = 72.dp,
    cardPadding = 20.dp,
    headerHeight = 56.dp,
    iconSize = 28.dp,
)

private val CaregiverDimensions = ContigoDimensions(
    buttonMinHeight = 56.dp,
    cardPadding = 16.dp,
    headerHeight = 48.dp,
    iconSize = 24.dp,
)

private val ContigoShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun ContigoTheme(
    isCaregiverMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    val typography = if (isCaregiverMode) CaregiverTypography else PatientTypography
    val dimensions = if (isCaregiverMode) CaregiverDimensions else PatientDimensions

    androidx.compose.runtime.CompositionLocalProvider(
        LocalExtraColors provides LightExtraColors,
        LocalDimensions provides dimensions
    ) {
        MaterialTheme(
            colorScheme = LightColors,
            typography = typography,
            shapes = ContigoShapes,
            content = content,
        )
    }
}

object ContigoTheme {
    val extraColors: ContigoExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtraColors.current

    val dimensions: ContigoDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalDimensions.current
}
