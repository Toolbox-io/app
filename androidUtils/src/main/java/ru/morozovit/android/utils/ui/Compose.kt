@file:Suppress("NOTHING_TO_INLINE")

package ru.morozovit.android.utils.ui

import android.widget.ImageView.ScaleType
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.constraintlayout.compose.ConstrainScope
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayoutBaseScope
import androidx.constraintlayout.compose.HorizontalAnchorable
import androidx.constraintlayout.compose.VerticalAnchorable
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass

inline fun <T> compositionLocalOf(
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy()
) = compositionLocalOf(policy) { error("Not initialized") }

/**
 * Adds a vertical scroll modifier using [rememberScrollState].
 */
inline fun Modifier.verticalScroll() = composed { verticalScroll(rememberScrollState()) }

/**
 * Applies a modifier conditionally.
 * @param condition If true, adds [block] to this modifier.
 * @param block The modifier to apply.
 * @return The resulting [Modifier].
 */
inline fun Modifier.applyIf(
    condition: Boolean,
    block: (Modifier) -> Modifier
) = if (condition) this + block(this) else this

/**
 * Copies [PaddingValues], optionally overriding individual sides.
 * @param start Override for start padding.
 * @param end Override for end padding.
 * @param top Override for top padding.
 * @param bottom Override for bottom padding.
 * @return The copied [PaddingValues].
 */
@Composable
inline fun PaddingValues.copy(
    start: Dp? = null,
    end: Dp? = null,
    top: Dp? = null,
    bottom: Dp? = null
) = PaddingValues(
    start = start ?: calculateStartPadding(LocalLayoutDirection.current),
    end = end ?: calculateEndPadding(LocalLayoutDirection.current),
    top = top ?: calculateTopPadding(),
    bottom = bottom ?: calculateBottomPadding()
)

/**
 * Copies [PaddingValues], optionally overriding horizontal and vertical padding.
 * @param horizontal Override for start and end padding.
 * @param vertical Override for top and bottom padding.
 * @return The copied [PaddingValues].
 */
@Composable
inline fun PaddingValues.copy(
    horizontal: Dp? = null,
    vertical: Dp? = null
) = copy(
    start = horizontal,
    end = horizontal,
    top = vertical,
    bottom = vertical
)

typealias WidthSizeClass = WindowWidthSizeClass
typealias HeightSizeClass = WindowHeightSizeClass
typealias SizeClass = WindowSizeClass

/**
 * Gets the raw integer value for a [WindowWidthSizeClass].
 */
val WindowWidthSizeClass.rawValue get() = when (this) {
    WindowWidthSizeClass.COMPACT -> 0
    WindowWidthSizeClass.MEDIUM -> 1
    WindowWidthSizeClass.EXPANDED -> 2
    else -> throw IllegalArgumentException("Unsupported WindowWidthSizeClass: $this")
}

/**
 * Gets the raw integer value for a [WindowHeightSizeClass].
 */
val WindowHeightSizeClass.rawValue get() = when (this) {
    WindowHeightSizeClass.COMPACT -> 0
    WindowHeightSizeClass.MEDIUM -> 1
    WindowHeightSizeClass.EXPANDED -> 2
    else -> throw IllegalArgumentException("Unsupported WindowHeightSizeClass: $this")
}

/**
 * Compares two [WindowWidthSizeClass] values.
 *
 * @param other The other value to compare to.
 */
inline operator fun WindowWidthSizeClass.compareTo(other: WindowWidthSizeClass) = rawValue.compareTo(other.rawValue)
/**
 * Compares two [WindowHeightSizeClass] values.
 *
 * @param other The other value to compare to.
 */
inline operator fun WindowHeightSizeClass.compareTo(other: WindowHeightSizeClass) = rawValue.compareTo(other.rawValue)

/**
 * Gets the width size class from a [WindowSizeClass].
 */
inline val WindowSizeClass.width get() = windowWidthSizeClass
/**
 * Gets the height size class from a [WindowSizeClass].
 */
inline val WindowSizeClass.height get() = windowHeightSizeClass

/**
 * Gets the width size class from a [WindowAdaptiveInfo].
 */
inline val WindowAdaptiveInfo.widthSizeClass get() = windowSizeClass.width
/**
 * Gets the height size class from a [WindowAdaptiveInfo].
 */
inline val WindowAdaptiveInfo.heightSizeClass get() = windowSizeClass.height

/**
 * Returns the current value of a [CompositionLocal].
 */
@Composable
inline operator fun <T> CompositionLocal<T>.invoke() = current

/**
 * Maps a [ContentScale] to an Android [ScaleType].
 * @return The corresponding [ScaleType], or null if not mappable.
 */
fun ContentScale.asAndroidScaleType() = when (this) {
    ContentScale.Fit -> ScaleType.FIT_CENTER
    ContentScale.Crop -> ScaleType.CENTER_CROP
    ContentScale.FillWidth,
    ContentScale.FillHeight,
    ContentScale.FillBounds -> ScaleType.FIT_XY
    ContentScale.Inside -> ScaleType.CENTER_INSIDE
    else -> null
}

/**
 * Returns the absolute left anchor for a [ConstrainScope].
 */
inline val ConstrainScope.left get() = absoluteLeft
/**
 * Returns the absolute right anchor for a [ConstrainScope].
 */
inline val ConstrainScope.right get() = absoluteRight

/**
 * Returns the absolute left anchor for a [ConstrainedLayoutReference].
 */
inline val ConstrainedLayoutReference.left get() = absoluteLeft
/**
 * Returns the absolute right anchor for a [ConstrainedLayoutReference].
 */
inline val ConstrainedLayoutReference.right get() = absoluteRight

/**
 * Links a [HorizontalAnchorable] to a horizontal anchor.
 *
 * @param anchor The anchor to link to.
 */
inline infix fun HorizontalAnchorable.link(
    anchor: ConstraintLayoutBaseScope.HorizontalAnchor
) = linkTo(anchor)

/**
 * Links a [VerticalAnchorable] to a vertical anchor.
 *
 * @param anchor The anchor to link to.
 */
inline infix fun VerticalAnchorable.link(
    anchor: ConstraintLayoutBaseScope.VerticalAnchor
) = linkTo(anchor)

/**
 * Adds two [Modifier]s together.
 */
inline operator fun Modifier.plus(other: Modifier) = then(other)

/**
 * Modifier that clears focus when the keyboard is dismissed.
 */
@OptIn(ExperimentalLayoutApi::class)
fun Modifier.clearFocusOnKeyboardDismiss() = composed {
    var isFocused by remember { mutableStateOf(false) }
    var keyboardAppearedSinceLastFocused by remember { mutableStateOf(false) }

    if (isFocused) {
        val imeIsVisible = WindowInsets.isImeVisible
        val focusManager = LocalFocusManager()

        LaunchedEffect(imeIsVisible) {
            if (imeIsVisible) {
                keyboardAppearedSinceLastFocused = true
            } else if (keyboardAppearedSinceLastFocused) {
                focusManager.clearFocus()
            }
        }
    }

    // The modifier
    onFocusEvent {
        if (isFocused != it.isFocused) {
            isFocused = it.isFocused
            if (isFocused) {
                keyboardAppearedSinceLastFocused = false
            }
        }
    }
}