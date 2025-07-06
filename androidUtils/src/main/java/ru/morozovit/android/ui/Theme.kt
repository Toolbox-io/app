package ru.morozovit.android.ui

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import ru.morozovit.android.invoke
import ru.morozovit.android.plus

enum class ThemeSetting {
    AsSystem,
    Light,
    Dark
}

interface WindowInsetsScope {
    val safeDrawingInsets: WindowInsets

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
fun Theme(
    modifier: Modifier = Modifier,
    dynamicThemeEnabled: Boolean,
    theme: ThemeSetting,
    darkColorScheme: ColorScheme,
    lightColorScheme: ColorScheme,
    darkTheme: Boolean = when (theme) {
        ThemeSetting.AsSystem -> isSystemInDarkTheme()
        ThemeSetting.Light -> false
        ThemeSetting.Dark -> true
    },
    consumeTopInsets: Boolean = false,
    consumeBottomInsets: Boolean = false,
    consumeLeftInsets: Boolean = false,
    consumeRightInsets: Boolean = false,
    enforceNavContrast: Boolean = false,
    setBackground: Boolean = true,
    configInsets: Boolean = true,
    content: @Composable WindowInsetsScope.() -> Unit
) {
    with (LocalContext()) {
        var colorScheme = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dynamicThemeEnabled ->
                if (darkTheme)
                    dynamicDarkColorScheme(this)
                else
                    dynamicLightColorScheme(this)
            darkTheme -> darkColorScheme
            else -> lightColorScheme
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

        if (this is Activity) {
            WindowCompat.getInsetsController(window, LocalView()).isAppearanceLightStatusBars = !darkTheme

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = enforceNavContrast
            }
        }

        MaterialTheme(colorScheme = colorScheme) {
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

            val actualContent = @Composable {
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
                        override val topInset: Int get() = topInset
                        override val bottomInset: Int get() = bottomInset
                        override val leftInset: Int get() = leftInset
                        override val rightInset: Int get() = rightInset
                    }
                )
            }

            val insetHandlerContent = @Composable {
                if (setBackground) {
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
                        actualContent()
                    }
                } else {
                    actualContent()
                }
            }

            if (configInsets) {
                WindowInsetsHandler(modifier, !consumeLeftInsets, !consumeRightInsets) {
                    insetHandlerContent()
                }
            } else {
                insetHandlerContent()
            }
        }
    }
}

@Suppress("AnimateAsStateLabel")
@Composable
fun OverlayTheme(
    modifier: Modifier = Modifier,
    dynamicThemeEnabled: Boolean,
    theme: ThemeSetting,
    darkColorScheme: ColorScheme,
    lightColorScheme: ColorScheme,
    darkTheme: Boolean = when (theme) {
        ThemeSetting.AsSystem -> isSystemInDarkTheme()
        ThemeSetting.Light -> false
        ThemeSetting.Dark -> true
    },
    content: @Composable () -> Unit
) {
    with (LocalContext()) {
        var colorScheme = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dynamicThemeEnabled ->
                if (darkTheme)
                    dynamicDarkColorScheme(this)
                else
                    dynamicLightColorScheme(this)
            darkTheme -> darkColorScheme
            else -> lightColorScheme
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

        MaterialTheme(colorScheme = colorScheme) {
            Surface(
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                content = content,
                modifier = modifier
            )
        }
    }
}

@Composable
inline fun WindowInsetsHandler(
    modifier: Modifier = Modifier,
    handleLeft: Boolean = true,
    handleRight: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier.let {
            var mod: Modifier = it
            if (handleLeft || handleRight) {
                mod += Modifier.windowInsetsPadding(
                    WindowInsets.displayCutout.only(
                        when {
                            handleLeft && !handleRight -> WindowInsetsSides.Left
                            !handleLeft -> WindowInsetsSides.Right
                            else -> WindowInsetsSides.Left + WindowInsetsSides.Right
                        }
                    ),
                )
            }
            mod += modifier

            mod
        },
        content = content
    )
}