package ru.morozovit.ultimatesecurity.ui

import android.os.Build
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection

interface WindowInsetsScope {
    val systemBarInsets: WindowInsets
    val isWindowInsetsConsumed: Boolean

    val topInset: Int
    val bottomInset: Int
    val leftInset: Int
    val rightInset: Int
}

@Composable
inline fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    consumeWindowInsets: Boolean = false,
    crossinline content: @Composable WindowInsetsScope.() -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        val insets = WindowInsets(
            WindowInsets.systemBars.getLeft(
                LocalDensity.current,
                LocalLayoutDirection.current
            ),
            WindowInsets.systemBars.getTop(LocalDensity.current),
            WindowInsets.systemBars.getRight(
                LocalDensity.current,
                LocalLayoutDirection.current
            ),
            WindowInsets.systemBars.getBottom(LocalDensity.current)
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
            val topInset = insets.getTop(LocalDensity.current)
            val bottomInset = insets.getBottom(LocalDensity.current)
            val leftInset = insets.getLeft(LocalDensity.current, LocalLayoutDirection.current)
            val rightInset = insets.getRight(LocalDensity.current, LocalLayoutDirection.current)

            content(object: WindowInsetsScope {
                override val systemBarInsets: WindowInsets
                    get() = insets
                override val isWindowInsetsConsumed: Boolean
                    get() = consumeWindowInsets
                override val topInset: Int
                    get() = topInset
                override val bottomInset: Int
                    get() = bottomInset
                override val leftInset: Int
                    get() = leftInset
                override val rightInset: Int
                    get() = rightInset
            })
        }
    }
}