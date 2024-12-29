@file:Suppress("unused", "NOTHING_TO_INLINE")
package ru.morozovit.android

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.content.res.Resources
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.service.quicksettings.Tile
import android.text.Editable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.Window
import android.view.animation.AnimationUtils.loadAnimation
import android.widget.EditText
import androidx.activity.result.ActivityResult
import androidx.annotation.AnimRes
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.TextUnit
import androidx.constraintlayout.compose.ConstrainScope
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayoutBaseScope
import androidx.constraintlayout.compose.HorizontalAnchorable
import androidx.constraintlayout.compose.VerticalAnchorable
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.regex.Pattern
import kotlin.reflect.KClass


val screenWidth: Int inline get() = Resources.getSystem().displayMetrics.widthPixels
val screenHeight: Int inline get() = Resources.getSystem().displayMetrics.heightPixels

fun appName(context: Context, packageName: String): String? {
    try {
        val packageManager = context.packageManager
        val applicationInfo = packageManager.getApplicationInfo(
            packageName, PackageManager.GET_META_DATA
        )
        val appName = packageManager.getApplicationLabel(applicationInfo) as String
        return appName
    } catch (e: PackageManager.NameNotFoundException) {
        return null
    }
}

fun Context.homeScreen() {
    val intentHome = Intent(ACTION_MAIN)
    intentHome.addCategory(Intent.CATEGORY_HOME)
    intentHome.flags = FLAG_ACTIVITY_NEW_TASK
    startActivity(intentHome)
}

fun Context.fileExists(contentUri: Uri): Boolean {
    try {
        val file = DocumentFile.fromTreeUri(this, contentUri)!!
        return file.exists()
    } catch (_: Exception) {
        return false
    }
}

inline fun Context.fileExists(contentUri: String) = fileExists(Uri.parse(contentUri))

inline fun Fragment.homeScreen() = requireActivity().homeScreen()

inline fun Editable?.toInt() = toString().toInt()
inline fun EditText.toInt() = text.toInt()

val View.screenX: Int get() {
    val arr = intArrayOf(0, 0)
    getLocationOnScreen(arr)
    return arr[0]
}

val View.screenY: Int get() {
    val arr = intArrayOf(0, 0)
    getLocationOnScreen(arr)
    return arr[1]
}

val View.relativeX: Int
    inline get() = (parent as ViewGroup).screenX - screenX


val View.relativeY: Int
    inline get() = (parent as ViewGroup).screenY - screenY

fun Activity.setWindowFlag(bits: Int, on: Boolean) {
    val win = window
    val winParams = win.attributes
    if (on) {
        winParams.flags = winParams.flags or bits
    } else {
        winParams.flags = winParams.flags and bits.inv()
    }
    win.attributes = winParams
}

fun async(exec: () -> Unit) = Thread(exec).apply {start()}

inline fun MaterialAlertDialogBuilder.setNeutralButton(text: CharSequence)
    = setNeutralButton(text, null)
inline fun MaterialAlertDialogBuilder.setNeutralButton(@StringRes textRes: Int)
    = setNeutralButton(textRes, null)

inline fun MaterialAlertDialogBuilder.setNegativeButton(text: CharSequence)
        = setNegativeButton(text, null)
inline fun MaterialAlertDialogBuilder.setNegativeButton(@StringRes textRes: Int)
        = setNegativeButton(textRes, null)

inline fun MaterialAlertDialogBuilder.setPositiveButton(text: CharSequence)
        = setPositiveButton(text, null)
inline fun MaterialAlertDialogBuilder.setPositiveButton(@StringRes textRes: Int)
        = setPositiveButton(textRes, null)

inline fun View.startAnimation(@AnimRes animRes: Int) = startAnimation(loadAnimation(context, animRes))

inline fun Tile.configure(apply: Tile.() -> Unit) {
    apply.invoke(this)
    updateTile()
}

fun View.createBlankClone(
    includeMargins: Boolean = true,
    fixedWidth: Boolean = false
): View {
    val v = View(context)


    var width = width
    var height = height

    fun setParams() {
        if (layoutParams is MarginLayoutParams && includeMargins) {
            val p = layoutParams as MarginLayoutParams
            width += p.leftMargin + p.rightMargin
            height += p.topMargin + p.bottomMargin
        }
        if (!fixedWidth) {
            if (layoutParams.width == MATCH_PARENT) {
                width = MATCH_PARENT
            }
            if (layoutParams.height == MATCH_PARENT) {
                height = MATCH_PARENT
            }
        }
    }

    if (layoutParams == null)
        addOneTimeOnGlobalLayoutListener(::setParams)
    else
        setParams()

    v.addOneTimeOnGlobalLayoutListener {
        updateLayoutParams {
            this.width = width
            this.height = height
        }
    }
    return v
}

inline fun View.addOneTimeOnGlobalLayoutListener(crossinline listener: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object: OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            listener()
            viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    })
}

inline fun View.addOneTimeOnPreDrawListener(crossinline listener: () -> Boolean) {
    viewTreeObserver.addOnPreDrawListener(object: OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            viewTreeObserver.removeOnPreDrawListener(this)
            return listener()
        }
    })
}

inline val View.parentViewGroup get() = parent as ViewGroup

inline fun View.removeSelf() = parentViewGroup.removeView(this)
inline val View.pos get() = parentViewGroup.indexOfChild(this)

fun Context.launchFiles(): Boolean {
    val primaryStorageUri = Uri.parse(
        "content://com.android.externalstorage.documents/root/primary"
    )
    fun launchIntent(intent: Intent): Boolean {
        try {
            startActivity(intent)
            return true
        } catch (th: Throwable) {
            return false
        }
    }
    fun launchIntentWithComponent(action: String, componentName: ComponentName? = null):
            Boolean {
        val intent = Intent(action, primaryStorageUri)
        if (componentName != null) {
            intent.setComponent(componentName)
        }
        return launchIntent(intent)
    }
    fun intent1() = launchIntentWithComponent(
        Intent.ACTION_VIEW,
        ComponentName(
            "com.google.android.documentsui",
            "com.android.documentsui.files.FilesActivity"
        )
    )
    fun intent2() = launchIntentWithComponent(
        Intent.ACTION_VIEW,
        ComponentName(
            "com.android.documentsui",
            "com.android.documentsui.files.FilesActivity"
        )
    )
    fun intent3() = launchIntentWithComponent(
        Intent.ACTION_VIEW,
        ComponentName(
            "com.android.documentsui",
            "com.android.documentsui.FilesActivity"
        )
    )
    fun intent4() = launchIntentWithComponent(Intent.ACTION_VIEW)
    fun intent5() = launchIntentWithComponent("android.provider.action.BROWSE")
    fun intent6() = launchIntentWithComponent("android.provider.action.BROWSE_DOCUMENT_ROOT")

    return intent1() || intent2() || intent3() || intent4() || intent5() || intent6()
}
inline fun Fragment.launchFiles() = requireActivity().launchFiles()

inline fun Fragment.getSystemService(name: String): Any? = requireActivity().getSystemService(name)
inline fun <T> Fragment.getSystemService(cls: Class<T>): T? = requireActivity().getSystemService(cls)

inline fun <T: Any> Context.getSystemService(cls: KClass<T>): T? = getSystemService(cls.java)
inline fun <T: Any> Fragment.getSystemService(cls: KClass<T>): T? = requireActivity().getSystemService(cls)

inline val Fragment.supportFragmentManager get() = requireActivity().supportFragmentManager

typealias ActivityLauncher = BetterActivityResult<Intent, ActivityResult>
typealias ActivityResultLauncher = ActivityLauncher

val AppCompatActivity.activityResultLauncher get() = BetterActivityResult.registerActivityForResult(this)



fun getOpenDocumentIntent(vararg fileTypes: String) =
    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        if (fileTypes.size == 1) {
            type = fileTypes[0]
        } else {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, fileTypes)
        }
    }

fun Context.getFileName(uri: Uri): String {
    if (uri.scheme == "content") {
        contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        )!!.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
    } else {
        throw UnsupportedOperationException("Cannot process non-\"content://\" URIs")
    }
    return uri.lastPathSegment.let { name ->
        name!!
        val matcher = Pattern.compile("\\w+:(.*/)*(.*)").matcher(name)
        if (matcher.find()) {
            matcher.group(2)
        } else {
            name
        }
    }
}

inline fun Fragment.getFileName(uri: Uri) = requireActivity().getFileName(uri)

val UsbInterface.endpointList: List<UsbEndpoint> get() {
    val list = mutableListOf<UsbEndpoint>()
    for (i in 0 until endpointCount) {
        list.add(getEndpoint(i))
    }
    return list.toList()
}

fun Int.toDp(resources: Resources) = this * resources.displayMetrics.density

val Fragment.window: Window? get() = requireActivity().window

fun Activity.resolveAttr(@AttrRes attr: Int): Int? {
    val typedValue = TypedValue()
    if (theme.resolveAttribute(attr,
            typedValue, true)) {
        val resId = typedValue.resourceId
        return if (resId != 0) resId else null
    } else {
        return null
    }
}

fun Fragment.resolveAttr(@AttrRes attr: Int) = requireActivity().resolveAttr(attr)

val Fragment.packageManager: PackageManager get() = requireActivity().packageManager

fun PackageManager.canRequestPackageInstallsOrFalse()
= if (Build.VERSION.SDK_INT >= 26) {
    canRequestPackageInstalls()
} else {
    false
}

@Suppress("UNUSED_PARAMETER")
class QuickAlertDialogBuilder(context: Context): MaterialAlertDialogBuilder(context) {
    fun title(title: CharSequence): QuickAlertDialogBuilder {
        setTitle(title)
        return this
    }
    fun title(@StringRes titleRes: Int): QuickAlertDialogBuilder {
        setTitle(titleRes)
        return this
    }

    fun message(message: CharSequence): QuickAlertDialogBuilder {
        setMessage(message)
        return this
    }
    fun message(@StringRes messageRes: Int): QuickAlertDialogBuilder {
        setMessage(messageRes)
        return this
    }

    fun body(body: CharSequence) = message(body)
    fun body(@StringRes bodyRes: Int) = message(bodyRes)

    inline fun positiveButton(
        text: CharSequence,
        crossinline listener: () -> Unit
    ) : QuickAlertDialogBuilder {
        setPositiveButton(text) { _, _ -> listener() }
        return this
    }
    inline fun positiveButton(@StringRes textRes: Int, crossinline listener: () -> Unit): QuickAlertDialogBuilder {
        setPositiveButton(textRes) { _, _ -> listener() }
        return this
    }
    inline fun positiveButton(text: CharSequence, listener: Nothing? = null): QuickAlertDialogBuilder {
        setPositiveButton(text, null)
        return this
    }
    inline fun positiveButton(@StringRes text: Int, listener: Nothing? = null): QuickAlertDialogBuilder {
        setPositiveButton(text, null)
        return this
    }

    inline fun negativeButton(text: CharSequence, crossinline listener: () -> Unit): QuickAlertDialogBuilder {
        setNegativeButton(text) { _, _ -> listener() }
        return this
    }
    inline fun negativeButton(@StringRes textRes: Int, crossinline listener: () -> Unit): QuickAlertDialogBuilder {
        setNegativeButton(textRes) { _, _ -> listener() }
        return this
    }
    inline fun negativeButton(text: CharSequence, listener: Nothing? = null): QuickAlertDialogBuilder {
        setNegativeButton(text, null)
        return this
    }
    inline fun negativeButton(@StringRes text: Int, listener: Nothing? = null): QuickAlertDialogBuilder {
        setNegativeButton(text, null)
        return this
    }

    inline fun neutralButton(text: CharSequence, crossinline listener: () -> Unit): QuickAlertDialogBuilder {
        setNeutralButton(text) { _, _ -> listener() }
        return this
    }
    inline fun neutralButton(@StringRes textRes: Int, crossinline listener: () -> Unit): QuickAlertDialogBuilder {
        setNeutralButton(textRes) { _, _ -> listener() }
        return this
    }
    inline fun neutralButton(text: CharSequence, listener: Nothing? = null): QuickAlertDialogBuilder {
        setNeutralButton(text, null)
        return this
    }
    inline fun neutralButton(@StringRes text: Int, listener: Nothing? = null): QuickAlertDialogBuilder {
        setNeutralButton(text, null)
        return this
    }

    inline fun onCancel(crossinline listener: () -> Unit): QuickAlertDialogBuilder {
        setOnCancelListener { listener() }
        return this
    }

    inline fun cancelable(value: Boolean): QuickAlertDialogBuilder {
        setCancelable(value)
        return this
    }
}

inline fun Activity.alertDialog(crossinline config: QuickAlertDialogBuilder.() -> Unit) {
    val builder = QuickAlertDialogBuilder(this)
    config(builder)
    builder.show()
}

inline fun Fragment.alertDialog(crossinline config: QuickAlertDialogBuilder.() -> Unit)
    = requireActivity().alertDialog(config)

class AuthenticationConfig {
    var success: ((BiometricPrompt.AuthenticationResult) -> Unit)? = null
    var fail: (() -> Unit)? = null
    var error: ((Int, String) -> Unit)? = null

    fun success(block: (BiometricPrompt.AuthenticationResult) -> Unit) {
        success = block
    }

    fun fail(block: () -> Unit) {
        fail = block
    }

    fun error(block: (Int, String) -> Unit) {
        error = block
    }

    lateinit var title: String
    var subtitle: String? = null
    lateinit var negativeButtonText: String
}

inline fun FragmentActivity.requestAuthentication(crossinline callback: AuthenticationConfig.() -> Unit): BiometricPrompt {
    val config = AuthenticationConfig()
    callback(config)
    try {
        config.title
        config.negativeButtonText
    } catch (e: UninitializedPropertyAccessException) {
        throw IllegalStateException("Required fields haven't been set")
    }
    val executor = ContextCompat.getMainExecutor(this)
    val biometricPrompt = BiometricPrompt(
        this,
        executor,
        object: BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                config.fail?.invoke()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                config.success?.invoke(result)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                config.error?.invoke(errorCode, "$errString")
            }
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder().apply {
        setTitle(config.title)
        setSubtitle(config.subtitle)
        setNegativeButtonText(config.negativeButtonText)
        setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        setConfirmationRequired(false)
    }.build()
    biometricPrompt.authenticate(promptInfo)
    return biometricPrompt
}

inline fun <T> applyAll(vararg items: T, callback: T.(Int) -> Unit) {
    items.forEach {
        it.apply {
            callback(this, items.indexOf(it))
        }
    }
}

inline fun BottomSheetBehavior<*>.addBottomSheetCallback(crossinline callback: (Int) -> Unit) {
    addBottomSheetCallback(object: BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            callback(newState)
        }
        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    })
}

@Composable
inline fun TextUnit.toDp() = with(LocalDensity()) { toDp() }

val ConstrainScope.left get() = absoluteLeft
val ConstrainScope.right get() = absoluteRight

val ConstrainedLayoutReference.left get() = absoluteLeft
val ConstrainedLayoutReference.right get() = absoluteRight

inline infix fun HorizontalAnchorable.link(
    anchor: ConstraintLayoutBaseScope.HorizontalAnchor
) = linkTo(anchor)

inline infix fun VerticalAnchorable.link(
    anchor: ConstraintLayoutBaseScope.VerticalAnchor
) = linkTo(anchor)

inline operator fun Modifier.plus(other: Modifier) = then(other)

data class PreviewUtils(
    val valueOrFalse: (() -> Boolean) -> Boolean,
    val runOrNoop: (() -> Unit) -> Unit,
    val isPreview: Boolean,
    val valueOrTrue: (() -> Boolean) -> Boolean
)

@Composable
inline fun previewUtils(): PreviewUtils {
    val isPreview = LocalInspectionMode()
    return PreviewUtils(
        valueOrFalse = { value ->
            if (isPreview) false else value()
        },
        runOrNoop = { block ->
            if (!isPreview) block()
        },
        isPreview = isPreview,
        valueOrTrue = { value ->
            if (isPreview) true else value()
        },
    )
}

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
    onFocusEvent {
        if (isFocused != it.isFocused) {
            isFocused = it.isFocused
            if (isFocused) {
                keyboardAppearedSinceLastFocused = false
            }
        }
    }
}

val unsupported: Nothing
    get() = throw UnsupportedOperationException()

val WindowWidthSizeClass.rawValue: Int get() =
    when (this) {
        WindowWidthSizeClass.COMPACT -> 0
        WindowWidthSizeClass.MEDIUM -> 1
        WindowWidthSizeClass.EXPANDED -> 2
        else -> throw IllegalArgumentException("Unsupported WindowWidthSizeClass: $this")
    }

val WindowHeightSizeClass.rawValue: Int get() =
    when (this) {
        WindowHeightSizeClass.COMPACT -> 0
        WindowHeightSizeClass.MEDIUM -> 1
        WindowHeightSizeClass.EXPANDED -> 2
        else -> throw IllegalArgumentException("Unsupported WindowHeightSizeClass: $this")
    }

operator fun WindowWidthSizeClass.compareTo(other: WindowWidthSizeClass) = this.rawValue.compareTo(other.rawValue)
operator fun WindowHeightSizeClass.compareTo(other: WindowHeightSizeClass) = this.rawValue.compareTo(other.rawValue)

val WindowSizeClass.width get() = windowWidthSizeClass
val WindowSizeClass.height get() = windowHeightSizeClass

val WindowAdaptiveInfo.widthSizeClass get() = windowSizeClass.width
val WindowAdaptiveInfo.heightSizeClass get() = windowSizeClass.height

typealias WidthSizeClass = WindowWidthSizeClass
typealias HeightSizeClass = WindowHeightSizeClass
typealias SizeClass = WindowSizeClass

@Composable
operator fun <T> CompositionLocal<T>.invoke() = current

