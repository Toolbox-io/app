package io.toolbox.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.toolbox.Settings
import ru.morozovit.android.utils.ui.OverlayTheme
import ru.morozovit.android.utils.ui.Theme
import ru.morozovit.android.utils.ui.ThemeSetting
import ru.morozovit.android.utils.ui.WindowInsetsScope

val appLightColorScheme by lazy {
    lightColorScheme(
        primary = Color(0xFF585992),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE2DFFF),
        onPrimaryContainer = Color(0xFF14134A),
        secondary = Color(0xFF5D5C72),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE2E0F9),
        onSecondaryContainer = Color(0xFF1A1A2C),
        tertiary = Color(0xFF795369),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD8EB),
        onTertiaryContainer = Color(0xFF2F1124),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFCF8FF),
        onBackground = Color(0xFF1B1B21),
        surface = Color(0xFFFCF8FF),
        onSurface = Color(0xFF1B1B21),
        surfaceVariant = Color(0xFFE4E1EC),
        onSurfaceVariant = Color(0xFF47464F),
        outline = Color(0xFF777680),
        outlineVariant = Color(0xFFC8C5D0),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF303036),
        inverseOnSurface = Color(0xFFF3EFF7),
        inversePrimary = Color(0xFFC1C1FF),
        surfaceDim = Color(0xFFDCD9E0),
        surfaceBright = Color(0xFFFCF8FF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF6F2FA),
        surfaceContainer = Color(0xFFF0ECF4),
        surfaceContainerHigh = Color(0xFFEAE7EF),
        surfaceContainerHighest = Color(0xFFE4E1E9)
    )
}

val appDarkColorScheme by lazy {
    darkColorScheme(
        primary = Color(0xFFC1C1FF),
        onPrimary = Color(0xFF2A2A60),
        primaryContainer = Color(0xFF404178),
        onPrimaryContainer = Color(0xFFE2DFFF),
        secondary = Color(0xFFC6C4DD),
        onSecondary = Color(0xFF2F2F42),
        secondaryContainer = Color(0xFF454559),
        onSecondaryContainer = Color(0xFFE2E0F9),
        tertiary = Color(0xFFE9B9D3),
        onTertiary = Color(0xFF46263A),
        tertiaryContainer = Color(0xFF5F3C51),
        onTertiaryContainer = Color(0xFFFFD8EB),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF131318),
        onBackground = Color(0xFFE4E1E9),
        surface = Color(0xFF131318),
        onSurface = Color(0xFFE4E1E9),
        surfaceVariant = Color(0xFF47464F),
        onSurfaceVariant = Color(0xFFC8C5D0),
        outline = Color(0xFF918F9A),
        outlineVariant = Color(0xFF47464F),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE4E1E9),
        inverseOnSurface = Color(0xFF303036),
        inversePrimary = Color(0xFF585992),
        surfaceDim = Color(0xFF131318),
        surfaceBright = Color(0xFF39383F),
        surfaceContainerLowest = Color(0xFF0E0E13),
        surfaceContainerLow = Color(0xFF1B1B21),
        surfaceContainer = Color(0xFF1F1F25),
        surfaceContainerHigh = Color(0xFF2A292F),
        surfaceContainerHighest = Color(0xFF35343A)
    )
}

var dynamicThemeEnabled by mutableStateOf(
    runCatching { Settings.materialYouEnabled }.getOrNull() == true
)
var theme by mutableStateOf(
    runCatching { Settings.appTheme }.getOrNull() ?: ThemeSetting.AsSystem
)

@Composable
inline fun AppTheme(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = when (theme) {
        ThemeSetting.AsSystem -> isSystemInDarkTheme()
        ThemeSetting.Light -> false
        ThemeSetting.Dark -> true
    },
    dynamicColor: Boolean = true,
    consumeTopInsets: Boolean = false,
    consumeBottomInsets: Boolean = false,
    consumeLeftInsets: Boolean = false,
    consumeRightInsets: Boolean = false,
    enforceNavContrast: Boolean = false,
    setBackground: Boolean = true,
    configInsets: Boolean = true,
    crossinline content: @Composable WindowInsetsScope.() -> Unit
) = Theme(
    modifier = modifier,
    lightColorScheme = appLightColorScheme,
    darkColorScheme = appDarkColorScheme,
    theme = theme,
    dynamicThemeEnabled = dynamicThemeEnabled && dynamicColor,
    darkTheme = darkTheme,
    consumeTopInsets = consumeTopInsets,
    consumeBottomInsets = consumeBottomInsets,
    consumeLeftInsets = consumeLeftInsets,
    consumeRightInsets = consumeRightInsets,
    enforceNavContrast = enforceNavContrast,
    setBackground = setBackground,
    configInsets = configInsets
) { content() }

@Suppress("AnimateAsStateLabel")
@Composable
inline fun OverlayAppTheme(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = when (theme) {
        ThemeSetting.AsSystem -> isSystemInDarkTheme()
        ThemeSetting.Light -> false
        ThemeSetting.Dark -> true
    },
    dynamicColor: Boolean = true,
    crossinline content: @Composable () -> Unit
) = OverlayTheme(
    modifier = modifier,
    lightColorScheme = appLightColorScheme,
    darkColorScheme = appDarkColorScheme,
    dynamicThemeEnabled = dynamicThemeEnabled && dynamicColor,
    theme = theme,
    darkTheme = darkTheme
) { content() }