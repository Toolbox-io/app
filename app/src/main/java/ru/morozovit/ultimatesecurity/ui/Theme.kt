package ru.morozovit.ultimatesecurity.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import ru.morozovit.android.invoke
import ru.morozovit.android.previewUtils
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.Settings.materialYouEnabled

val appLightColorScheme by lazy {
    lightColorScheme(
        primary = Color(0xFF845416),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDCBC),
        onPrimaryContainer = Color(0xFF2C1700),
        secondary = Color(0xFF725A42),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFEDDBE),
        onSecondaryContainer = Color(0xFF291805),
        tertiary = Color(0xFF56633B),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFDAE9B6),
        onTertiaryContainer = Color(0xFF151F01),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFFF8F4),
        onBackground = Color(0xFF211A14),
        surface = Color(0xFFFFF8F4),
        onSurface = Color(0xFF211A14),
        surfaceVariant = Color(0xFFF1DFD0),
        onSurfaceVariant = Color(0xFF50453A),
        outline = Color(0xFF827568),
        outlineVariant = Color(0xFFD5C4B5),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF372F28),
        inverseOnSurface = Color(0xFFFDEEE3),
        inversePrimary = Color(0xFFFBBA73),
        surfaceDim = Color(0xFFE6D8CC),
        surfaceBright = Color(0xFFFFF8F4),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFFF1E7),
        surfaceContainer = Color(0xFFFAEBE0),
        surfaceContainerHigh = Color(0xFFF4E6DA),
        surfaceContainerHighest = Color(0xFFEEE0D5),
    )
}

val appDarkColorScheme by lazy {
    darkColorScheme(
        primary = Color(0xFFFBBA73),
        onPrimary = Color(0xFF492900),
        primaryContainer = Color(0xFF683D00),
        onPrimaryContainer = Color(0xFFFFDCBC),
        secondary = Color(0xFFE0C1A3),
        onSecondary = Color(0xFF402D17),
        secondaryContainer = Color(0xFF58432C),
        onSecondaryContainer = Color(0xFFFEDDBE),
        tertiary = Color(0xFFBECC9C),
        onTertiary = Color(0xFF293411),
        tertiaryContainer = Color(0xFF3F4B26),
        onTertiaryContainer = Color(0xFFDAE9B6),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF19120C),
        onBackground = Color(0xFFEEE0D5),
        surface = Color(0xFF19120C),
        onSurface = Color(0xFFEEE0D5),
        surfaceVariant = Color(0xFF50453A),
        onSurfaceVariant = Color(0xFFD5C4B5),
        outline = Color(0xFF9D8E81),
        outlineVariant = Color(0xFF50453A),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFEEE0D5),
        inverseOnSurface = Color(0xFF372F28),
        inversePrimary = Color(0xFF845416),
        surfaceDim = Color(0xFF19120C),
        surfaceBright = Color(0xFF403830),
        surfaceContainerLowest = Color(0xFF130D07),
        surfaceContainerLow = Color(0xFF211A14),
        surfaceContainer = Color(0xFF251E17),
        surfaceContainerHigh = Color(0xFF302921),
        surfaceContainerHighest = Color(0xFF3B332C),
    )
}

enum class Theme {
    AsSystem,
    Light,
    Dark
}

var dynamicThemeEnabled by mutableStateOf(
    runCatching { materialYouEnabled }.getOrNull() ?: false
)
var theme by mutableStateOf(
    runCatching { Settings.appTheme }.getOrNull() ?: Theme.AsSystem
)

interface WindowInsetsScope {
    val systemBarInsets: WindowInsets
    val isWindowInsetsConsumed: Boolean

    val topInset: Int
    val bottomInset: Int
    val leftInset: Int
    val rightInset: Int
}

@Suppress("AnimateAsStateLabel")
@Composable
inline fun AppTheme(
    _darkTheme: Boolean = isSystemInDarkTheme(),
    consumeWindowInsets: Boolean = false,
    crossinline content: @Composable WindowInsetsScope.() -> Unit
) {
    val darkTheme = when (theme) {
        Theme.AsSystem -> _darkTheme
        Theme.Light -> false
        Theme.Dark -> true
    }
    var colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dynamicThemeEnabled -> {
            val context = LocalContext()
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> appDarkColorScheme
        else -> appLightColorScheme
    }

    val primary by animateColorAsState(colorScheme.primary)
    val onPrimary by animateColorAsState(colorScheme.onPrimary)
    val primaryContainer by animateColorAsState(colorScheme.primaryContainer)
    val onPrimaryContainer by animateColorAsState(colorScheme.onPrimaryContainer)
    val secondary by animateColorAsState(colorScheme.secondary)
    val onSecondary by animateColorAsState(colorScheme.onSecondary)
    val secondaryContainer by animateColorAsState(colorScheme.secondaryContainer)
    val onSecondaryContainer by animateColorAsState(colorScheme.onSecondaryContainer)
    val tertiary by animateColorAsState(colorScheme.tertiary)
    val onTertiary by animateColorAsState(colorScheme.onTertiary)
    val tertiaryContainer by animateColorAsState(colorScheme.tertiaryContainer)
    val onTertiaryContainer by animateColorAsState(colorScheme.onTertiaryContainer)
    val error by animateColorAsState(colorScheme.error)
    val onError by animateColorAsState(colorScheme.onError)
    val errorContainer by animateColorAsState(colorScheme.errorContainer)
    val onErrorContainer by animateColorAsState(colorScheme.onErrorContainer)
    val background by animateColorAsState(colorScheme.background)
    val onBackground by animateColorAsState(colorScheme.onBackground)
    val surface by animateColorAsState(colorScheme.surface)
    val onSurface by animateColorAsState(colorScheme.onSurface)
    val surfaceVariant by animateColorAsState(colorScheme.surfaceVariant)
    val onSurfaceVariant by animateColorAsState(colorScheme.onSurfaceVariant)
    val outline by animateColorAsState(colorScheme.outline)
    val outlineVariant by animateColorAsState(colorScheme.outlineVariant)
    val scrim by animateColorAsState(colorScheme.scrim)
    val inverseSurface by animateColorAsState(colorScheme.inverseSurface)
    val inverseOnSurface by animateColorAsState(colorScheme.inverseOnSurface)
    val inversePrimary by animateColorAsState(colorScheme.inversePrimary)
    val surfaceDim by animateColorAsState(colorScheme.surfaceDim)
    val surfaceBright by animateColorAsState(colorScheme.surfaceBright)
    val surfaceContainerLowest by animateColorAsState(colorScheme.surfaceContainerLowest)
    val surfaceContainerLow by animateColorAsState(colorScheme.surfaceContainerLow)
    val surfaceContainer by animateColorAsState(colorScheme.surfaceContainer)
    val surfaceContainerHigh by animateColorAsState(colorScheme.surfaceContainerHigh)
    val surfaceContainerHighest by animateColorAsState(colorScheme.surfaceContainerHighest)

    colorScheme = colorScheme.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        inversePrimary = inversePrimary,
        surfaceDim = surfaceDim,
        surfaceBright = surfaceBright,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest
    )

    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        val insets = WindowInsets(
            WindowInsets.systemBars.getLeft(
                LocalDensity(),
                LocalLayoutDirection()
            ),
            WindowInsets.systemBars.getTop(LocalDensity()),
            WindowInsets.systemBars.getRight(
                LocalDensity(),
                LocalLayoutDirection()
            ),
            WindowInsets.systemBars.getBottom(LocalDensity())
        )

        Surface(
            contentColor = colorScheme.onSurface,
            modifier = Modifier.let {
                val mod = it.fillMaxSize()
                if (consumeWindowInsets) {
                    mod.consumeWindowInsets(
                        WindowInsets.navigationBars.only(WindowInsetsSides.Vertical)
                    )
                }
                mod
            }
        ) {
            val topInset = insets.getTop(LocalDensity())
            val bottomInset = insets.getBottom(LocalDensity())
            val leftInset = insets.getLeft(LocalDensity(), LocalLayoutDirection())
            val rightInset = insets.getRight(LocalDensity(), LocalLayoutDirection())

            content(
                object: WindowInsetsScope {
                    override val systemBarInsets: WindowInsets get() = insets
                    override val isWindowInsetsConsumed: Boolean get() = consumeWindowInsets
                    override val topInset: Int get() = topInset
                    override val bottomInset: Int get() = bottomInset
                    override val leftInset: Int get() = leftInset
                    override val rightInset: Int get() = rightInset
                }
            )
        }
    }
}

@Composable
inline fun AppThemeIfNessecary(crossinline content: @Composable () -> Unit) {
    if (previewUtils().isPreview) {
        AppTheme {
            content()
        }
    } else {
        content()
    }
}