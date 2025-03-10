package io.toolbox.ui

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.toolbox.Settings
import ru.morozovit.android.invoke
import ru.morozovit.android.plus

val appLightColorScheme by lazy {
    lightColorScheme(
        primary = Color(0xFF8D4E2A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDBCA),
        onPrimaryContainer = Color(0xFF331100),
        secondary = Color(0xFF765848),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFDBCA),
        onSecondaryContainer = Color(0xFF2B160A),
        tertiary = Color(0xFF646032),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFEBE5AA),
        onTertiaryContainer = Color(0xFF1E1C00),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFFF8F6),
        onBackground = Color(0xFF221A15),
        surface = Color(0xFFFFF8F6),
        onSurface = Color(0xFF221A15),
        surfaceVariant = Color(0xFFF4DED4),
        onSurfaceVariant = Color(0xFF52443D),
        outline = Color(0xFF85746B),
        outlineVariant = Color(0xFFD7C2B9),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF382E29),
        inverseOnSurface = Color(0xFFFFEDE6),
        inversePrimary = Color(0xFFFFB68F),
        surfaceDim = Color(0xFFE8D7CF),
        surfaceBright = Color(0xFFFFF8F6),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFFF1EB),
        surfaceContainer = Color(0xFFFCEAE3),
        surfaceContainerHigh = Color(0xFFF6E5DD),
        surfaceContainerHighest = Color(0xFFF0DFD8),
    )
}

val appDarkColorScheme by lazy {
    darkColorScheme(
        primary = Color(0xFFFFB68F),
        onPrimary = Color(0xFF532201),
        primaryContainer = Color(0xFF703715),
        onPrimaryContainer = Color(0xFFFFDBCA),
        secondary = Color(0xFFE6BEAB),
        onSecondary = Color(0xFF432B1D),
        secondaryContainer = Color(0xFF5C4132),
        onSecondaryContainer = Color(0xFFFFDBCA),
        tertiary = Color(0xFFCEC891),
        onTertiary = Color(0xFF343108),
        tertiaryContainer = Color(0xFF4B481D),
        onTertiaryContainer = Color(0xFFEBE5AA),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF1A120D),
        onBackground = Color(0xFFF0DFD8),
        surface = Color(0xFF1A120D),
        onSurface = Color(0xFFF0DFD8),
        surfaceVariant = Color(0xFF52443D),
        onSurfaceVariant = Color(0xFFD7C2B9),
        outline = Color(0xFF9F8D84),
        outlineVariant = Color(0xFF52443D),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFF0DFD8),
        inverseOnSurface = Color(0xFF382E29),
        inversePrimary = Color(0xFF8D4E2A),
        surfaceDim = Color(0xFF1A120D),
        surfaceBright = Color(0xFF413732),
        surfaceContainerLowest = Color(0xFF140C09),
        surfaceContainerLow = Color(0xFF221A15),
        surfaceContainer = Color(0xFF271E19),
        surfaceContainerHigh = Color(0xFF322823),
        surfaceContainerHighest = Color(0xFF3D332E),
    )
}

enum class Theme {
    AsSystem,
    Light,
    Dark
}

var dynamicThemeEnabled by mutableStateOf(
    runCatching { Settings.materialYouEnabled }.getOrNull() == true
)
var theme by mutableStateOf(
    runCatching { Settings.appTheme }.getOrNull() ?: Theme.AsSystem
)

interface WindowInsetsScope {
    val safeDrawingInsets: WindowInsets
    val isWindowInsetsConsumed get() =
        isTopInsetConsumed &&
        isBottomInsetConsumed &&
        isLeftInsetConsumed &&
        isRightInsetConsumed

    val isTopInsetConsumed: Boolean
    val isBottomInsetConsumed: Boolean
    val isLeftInsetConsumed: Boolean
    val isRightInsetConsumed: Boolean

    val topInset: Int
    val bottomInset: Int
    val leftInset: Int
    val rightInset: Int
}

@Suppress("AnimateAsStateLabel")
@Composable
fun AppTheme(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = when (theme) {
        Theme.AsSystem -> isSystemInDarkTheme()
        Theme.Light -> false
        Theme.Dark -> true
    },
    consumeTopInsets: Boolean = false,
    consumeBottomInsets: Boolean = false,
    consumeLeftInsets: Boolean = false,
    consumeRightInsets: Boolean = false,
    enforceNavContrast: Boolean = false,
    content: @Composable WindowInsetsScope.() -> Unit
) {
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

    WindowCompat.getInsetsController(
        (LocalContext() as Activity).window,
        LocalView()
    ).isAppearanceLightStatusBars = !darkTheme

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        (LocalContext() as Activity).window.isNavigationBarContrastEnforced = enforceNavContrast
    }

    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        val insets = WindowInsets(
            WindowInsets.safeDrawing.getLeft(
                LocalDensity(),
                LocalLayoutDirection()
            ),
            WindowInsets.safeDrawing.getTop(LocalDensity()),
            WindowInsets.safeDrawing.getRight(
                LocalDensity(),
                LocalLayoutDirection()
            ),
            WindowInsets.safeDrawing.getBottom(LocalDensity())
        )

        WindowInsetsHandler(modifier, !consumeLeftInsets, !consumeRightInsets) {
            Surface(
                contentColor = colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxSize()
                    .let {
                        var mod = it
                        if (consumeTopInsets) {
                            mod += Modifier.consumeWindowInsets(WindowInsets.safeContent.only(WindowInsetsSides.Top))
                        }
                        if (consumeBottomInsets) {
                            mod += Modifier.consumeWindowInsets(WindowInsets.safeContent.only(WindowInsetsSides.Bottom))
                        }
                        if (!consumeLeftInsets) {
                            mod += Modifier.consumeWindowInsets(WindowInsets.safeContent.only(WindowInsetsSides.Left))
                        }
                        if (!consumeRightInsets) {
                            mod += Modifier.consumeWindowInsets(WindowInsets.safeContent.only(WindowInsetsSides.Right))
                        }
                        mod
                    }
            ) {
                val topInset = insets.getTop(LocalDensity())
                val bottomInset = insets.getBottom(LocalDensity())
                val leftInset = insets.getLeft(LocalDensity(), LocalLayoutDirection())
                val rightInset = insets.getRight(LocalDensity(), LocalLayoutDirection())

                content(
                    object : WindowInsetsScope {
                        override val safeDrawingInsets: WindowInsets get() = insets
                        override val isTopInsetConsumed: Boolean get() = consumeTopInsets
                        override val isBottomInsetConsumed: Boolean get() = consumeBottomInsets
                        override val isLeftInsetConsumed: Boolean get() = consumeLeftInsets
                        override val isRightInsetConsumed: Boolean get() = consumeRightInsets
                        override val isWindowInsetsConsumed
                            get() =
                                consumeTopInsets &&
                                consumeBottomInsets &&
                                consumeLeftInsets &&
                                consumeRightInsets
                        override val topInset: Int get() = topInset
                        override val bottomInset: Int get() = bottomInset
                        override val leftInset: Int get() = leftInset
                        override val rightInset: Int get() = rightInset
                    }
                )
            }
        }
    }
}

@Composable
inline fun WindowInsetsHandler(
    modifier: Modifier = Modifier,
    handleLeft: Boolean = true,
    handleRight: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        Modifier.let {
            if (handleLeft || handleRight) {
                it.windowInsetsPadding(
                    WindowInsets.displayCutout.only(
                        when {
                            handleLeft && !handleRight -> WindowInsetsSides.Left
                            !handleLeft -> WindowInsetsSides.Right
                            else -> WindowInsetsSides.Left + WindowInsetsSides.Right
                        }
                    ),
                )
            } else {
                it
            }
        } + modifier
    ) {
        content()
    }
}