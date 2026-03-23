package com.example.smartmeal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.smartmeal.R

/**
 * Senior-level font configuration using Google Fonts.
 * This avoids bundling large TTF files and ensures the latest font versions.
 */

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Montserrat для букв и основного интерфейса
val MontserratFontName = GoogleFont("Montserrat")
val MontserratFontFamily = FontFamily(
    Font(googleFont = MontserratFontName, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = MontserratFontName, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = MontserratFontName, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = MontserratFontName, fontProvider = provider, weight = FontWeight.Bold)
)

// Mallanna для цифр
val MallannaFontName = GoogleFont("Mallanna")
val MallannaFontFamily = FontFamily(
    Font(googleFont = MallannaFontName, fontProvider = provider, weight = FontWeight.Normal)
)

// Material 3 Typography setup
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = MontserratFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = MontserratFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = MontserratFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = MontserratFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    labelLarge = TextStyle(
        fontFamily = MontserratFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)
