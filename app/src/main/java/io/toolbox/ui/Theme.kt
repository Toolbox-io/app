package io.toolbox.ui

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
    with (LocalContext() as Activity) {
        var colorScheme = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dynamicThemeEnabled ->
                if (darkTheme)
                    dynamicDarkColorScheme(this)
                else
                    dynamicLightColorScheme(this)
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

        WindowCompat.getInsetsController(window, LocalView()).isAppearanceLightStatusBars = !darkTheme

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = enforceNavContrast
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