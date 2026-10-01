package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SaiBluePrimaryDark,
    onPrimary = SaiBlueOnPrimaryDark,
    primaryContainer = SaiBlueContainerDark,
    onPrimaryContainer = SaiBlueOnContainerDark,
    background = SaiBackgroundDark,
    onBackground = SaiOnBackgroundDark,
    surface = SaiSurfaceDark,
    onSurface = SaiOnSurfaceDark,
    surfaceVariant = SaiSurfaceVariantDark,
    onSurfaceVariant = SaiOnSurfaceVariantDark,
)

private val LightColorScheme = lightColorScheme(
    primary = SaiBluePrimary,
    onPrimary = SaiBlueOnPrimary,
    primaryContainer = SaiBlueContainer,
    onPrimaryContainer = SaiBlueOnContainer,
    secondary = SaiSecondary,
    onSecondary = SaiOnSecondary,
    secondaryContainer = SaiSecondaryContainer,
    onSecondaryContainer = SaiOnSecondaryContainer,
    background = SaiBackground,
    onBackground = SaiOnBackground,
    surface = SaiSurface,
    onSurface = SaiOnSurface,
    surfaceVariant = SaiSurfaceVariant,
    onSurfaceVariant = SaiOnSurfaceVariant,
    outline = SaiOutline,
    outlineVariant = SaiOutlineVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to preserve the signature white background with subtle blue accents
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
