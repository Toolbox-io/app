@file:Suppress( "NOTHING_TO_INLINE", "SameParameterValue", "unused")
package ru.morozovit.android

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.CATEGORY_LAUNCHER
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.media.MediaPlayer
import android.net.Uri
import android.os.BaseBundle
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.os.PersistableBundle
import android.provider.OpenableColumns
import android.service.quicksettings.Tile
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver.OnPreDrawListener
import android.webkit.WebView
import android.widget.ImageView.ScaleType
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.constraintlayout.compose.ConstrainScope
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayoutBaseScope
import androidx.constraintlayout.compose.HorizontalAnchorable
import androidx.constraintlayout.compose.VerticalAnchorable
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LoggingConfig
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.Configuration
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import java.io.InputStream
import java.io.Serializable
import java.lang.ref.WeakReference
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Returns the width of the device screen in pixels.
 */
inline val screenWidth get() = Resources.getSystem().displayMetrics.widthPixels
/**
 * Returns the height of the device screen in pixels.
 */
inline val screenHeight get() = Resources.getSystem().displayMetrics.heightPixels

/**
 * Gets the application label (name) for the given package name.
 *
 * @param context The context to use for package manager lookup.
 * @param packageName The package name to look up.
 * @return The application label, or `null` if not found.
 */
fun appName(context: Context, packageName: String): String? {
    try {
        val packageManager = context.packageManager
        return packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(
                packageName, PackageManager.GET_META_DATA
            )
        ).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        return null
    }
}

/**
 * Launches the home screen activity.
 */
inline fun Context.homeScreen() = startActivity(
    Intent(ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
        flags = FLAG_ACTIVITY_NEW_TASK
    }
)

/**
 * Configures a [Tile] and updates it.
 * @param apply Lambda to apply configuration to the tile.
 */
inline fun Tile.configure(apply: Tile.() -> Unit) {
    apply.invoke(this)
    updateTile()
}

/**
 * Adds a one-time [OnPreDrawListener] to a [View].
 * As soon as the underlying listener is called, it's removed 
 * and the lambda is called.
 * 
 * @param listener Lambda returning true to proceed with drawing, false to cancel.
 */
inline fun View.addOneTimeOnPreDrawListener(crossinline listener: () -> Boolean) {
    viewTreeObserver.addOnPreDrawListener(object: OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            viewTreeObserver.removeOnPreDrawListener(this)
            return listener()
        }
    })
}

/**
 * Attempts to launch a file manager for the primary storage. Tries several known intents.
 * @return True if a file manager was successfully launched, false otherwise.
 */
fun Context.launchFiles(): Boolean {
    val primaryStorageUri = "content://com.android.externalstorage.documents/root/primary".toUri()

    fun launchIntentWithComponent(action: String, componentName: ComponentName? = null): Boolean {
        val intent = Intent(action, primaryStorageUri)
        if (componentName != null) {
            intent.setComponent(componentName)
        }
        try {
            startActivity(intent)
            return true
        } catch (_: Throwable) {
            return false
        }
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

/**
 * Gets a system service by name from a [Fragment].
 * 
 * @param name The service name
 */
inline fun Fragment.getSystemService(name: String): Any? = requireActivity().getSystemService(name)
/**
 * Gets a system service by Java class from a [Fragment].
 * 
 * @param cls The service Java class.
 */
inline fun <T> Fragment.getSystemService(cls: Class<T>): T? = requireActivity().getSystemService(cls)
/**
 * Gets a system service by [KClass] from a [Context].
 * 
 * @param cls The service Kotlin class.
 */
inline fun <T: Any> Context.getSystemService(cls: KClass<T>): T? = getSystemService(cls.java)
/**
 * Gets a system service by [KClass] from a [Fragment].
 * 
 * @param cls The service Kotlin class.
 */
inline fun <T: Any> Fragment.getSystemService(cls: KClass<T>): T? = requireActivity().getSystemService(cls)

/**
 * Type alias for a better activity result launcher.
 */
typealias ActivityLauncher = BetterActivityResult<Intent, ActivityResult>

/**
 * Gets a launcher for activity results from an [ActivityResultCaller].
 * 
 * MUST be called before any UI is initialized.
 */
inline val ActivityResultCaller.activityResultLauncher get() = BetterActivityResult.registerActivityForResult(this)

/**
 * Creates an intent to open a document, optionally filtering by file types.
 * @param fileTypes The MIME types to filter for.
 * @return The intent for opening a document.
 */
@Suppress("unused")
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

/**
 * Gets the display name of a file from a content URI.
 * @param uri The content URI.
 * @return The file name.
 * @throws UnsupportedOperationException if the URI is not a content URI.
 */
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

/**
 * Gets the display name of a file from a content URI in a [Fragment].
 * 
 * @param uri The `content://` URI.
 */
inline fun Fragment.getFileName(uri: Uri) = requireActivity().getFileName(uri)

/**
 * Returns a list of all endpoints for a [UsbInterface].
 */
val UsbInterface.endpointList: List<UsbEndpoint> get() {
    val list = mutableListOf<UsbEndpoint>()
    for (i in 0 until endpointCount) {
        list.add(getEndpoint(i))
    }
    return list.toList()
}

/**
 * Resolves an attribute resource ID from the current theme.
 * @param id The attribute resource ID.
 * @return The resolved resource ID, or `null` if not found.
 */
fun Activity.resolveAttr(@AttrRes id: Int): Int? {
    val typedValue = TypedValue()
    return if (theme.resolveAttribute(id, typedValue, true)) {
        typedValue.resourceId.takeIf { it != 0 }
    } else {
        null
    }
}

/**
 * A MaterialAlertDialogBuilder with a fluent API for quick configuration.
 * 
 * @param context The [Context] to use for the builder.
 */
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

/**
 * Shows an alert dialog in an [Activity] using a [QuickAlertDialogBuilder].
 * @param config Lambda to configure the dialog.
 * @return The shown [AlertDialog].
 */
inline fun Activity.alertDialog(crossinline config: QuickAlertDialogBuilder.() -> Unit): AlertDialog {
    val builder = QuickAlertDialogBuilder(this)
    config(builder)
    return builder.show()
}

/**
 * Shows an alert dialog in a [Fragment] using a [QuickAlertDialogBuilder].
 * @param config Lambda to configure the dialog.
 * @return The shown [AlertDialog].
 */
inline fun Fragment.alertDialog(
    crossinline config: QuickAlertDialogBuilder.() -> Unit
) = requireActivity().alertDialog(config)

/**
 * Configuration for biometric authentication prompts.
 */
class AuthenticationConfig {
    var success: ((BiometricPrompt.AuthenticationResult) -> Unit)? = null
    var fail: (() -> Unit)? = null
    var error: ((Int, String) -> Unit)? = null
    var always: (() -> Unit)? = null

    fun success(block: (BiometricPrompt.AuthenticationResult) -> Unit) {
        success = block
    }

    inline fun ail(noinline block: () -> Unit) {
        fail = block
    }

    inline fun error(noinline block: (Int, String) -> Unit) {
        error = block
    }

    inline fun always(noinline block: () -> Unit) {
        always = block
    }

    lateinit var title: String
    var subtitle: String? = null
    lateinit var negativeButtonText: String
}

/**
 * Requests biometric authentication in a [FragmentActivity] with a custom [AuthenticationConfig].
 * @param callback Lambda to configure the authentication prompt and callbacks.
 * @return The [BiometricPrompt] instance.
 * @throws IllegalStateException if required fields are not set.
 */
inline fun FragmentActivity.requestAuthentication(crossinline callback: AuthenticationConfig.() -> Unit): BiometricPrompt {
    val config = AuthenticationConfig()
    callback(config)
    try {
        config.title
        config.negativeButtonText
    } catch (_: UninitializedPropertyAccessException) {
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
                config.always?.invoke()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                config.error?.invoke(errorCode, "$errString")
                config.always?.invoke()
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
    onFocusEvent {
        if (isFocused != it.isFocused) {
            isFocused = it.isFocused
            if (isFocused) {
                keyboardAppearedSinceLastFocused = false
            }
        }
    }
}

/**
 * Throws [UnsupportedOperationException] when accessed.
 * 
 * It is very useful for [Serializable] data classes that need 
 * to have default values for params, but they must be specified.
 */
inline val unsupported: Nothing get() = throw UnsupportedOperationException()

/**
 * Gets the raw integer value for a [WindowWidthSizeClass].
 */
val WindowWidthSizeClass.rawValue get() =
    when (this) {
        WindowWidthSizeClass.COMPACT -> 0
        WindowWidthSizeClass.MEDIUM -> 1
        WindowWidthSizeClass.EXPANDED -> 2
        else -> throw IllegalArgumentException("Unsupported WindowWidthSizeClass: $this")
    }

/**
 * Gets the raw integer value for a [WindowHeightSizeClass].
 */
val WindowHeightSizeClass.rawValue get() =
    when (this) {
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
fun ContentScale.asAndroidScaleType(): ScaleType? {
    return when (this) {
        ContentScale.Fit -> ScaleType.FIT_CENTER
        ContentScale.Crop -> ScaleType.CENTER_CROP
        ContentScale.FillWidth,
        ContentScale.FillHeight,
        ContentScale.FillBounds -> ScaleType.FIT_XY
        ContentScale.Inside -> ScaleType.CENTER_INSIDE
        else -> null
    }
}

/**
 * Opens a URL in a browser from a [Context].
 * @param url The [Uri] to open.
 */
inline fun Context.openUrl(url: Uri) = startActivity(Intent(Intent.ACTION_VIEW, url))
/**
 * Opens a URL in a browser from a [Context].
 * @param url The string URL to open. Adds `https://` if the scheme is missing.
 */
inline fun Context.openUrl(url: String) = openUrl(
    url.let {
        if (!it.matches("^[\\w-_]://".toRegex())) {
            "https://$it"
        } else {
            it
        }
    }.toUri()
)

/**
 * Creates a [ComposeView] and sets its content.
 * @param lifecycleOwner Optional lifecycle owner to attach.
 * @param init Composable content to set.
 * @return The created [ComposeView].
 */
inline fun Context.ComposeView(lifecycleOwner: ComposeLifecycleOwner? = null, crossinline init: @Composable () -> Unit) =
    ComposeView(this).apply {
        setContent {
            init()
        }
        if (lifecycleOwner != null) {
            lifecycleOwner.onStart()
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        }
    }

/**
 * Creates a [ComposeView] and sets its content.
 * @param context The context to use.
 * @param init Composable content to set.
 */
inline fun ComposeView(context: Context, crossinline init: @Composable () -> Unit) {
    ComposeView(context).apply {
        setContent {
            init()
        }
    }
}

/**
 * Creates an [OnBackPressedCallback].
 * @param enabled Whether the callback is enabled.
 * @param callback The handler to invoke on back pressed.
 * @return The created [OnBackPressedCallback].
 */
inline fun backCallback(
    enabled: Boolean = true,
    crossinline callback: OnBackPressedCallback.() -> Unit
) = object: OnBackPressedCallback(enabled) {
    override fun handleOnBackPressed() {
        callback(this)
    }
}

/**
 * Encrypts the string using AES with a password-derived key.
 * @param password The password to use for encryption.
 * @return The encrypted string, including salt and IV.
 */
fun String.encrypt(password: String): String {
    val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
    val spec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val secretKey = factory.generateSecret(spec)

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val iv = ByteArray(cipher.blockSize).also { SecureRandom().nextBytes(it) }
    val params = IvParameterSpec(iv)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, params)

    val encryptedBytes = cipher.doFinal(toByteArray())
    return "${Base64.encodeToString(salt, Base64.DEFAULT)}:${Base64.encodeToString(iv, Base64.DEFAULT)}:${Base64.encodeToString(encryptedBytes, Base64.DEFAULT)}"
}

/**
 * Decrypts a string encrypted with [encrypt].
 * @param password The password to use for decryption.
 * @return The decrypted string.
 */
fun String.decrypt(password: String): String {
    val parts = split(":")
    val salt = Base64.decode(parts[0], Base64.DEFAULT)
    val iv = Base64.decode(parts[1], Base64.DEFAULT)
    val encryptedBytes = Base64.decode(parts[2], Base64.DEFAULT)

    val spec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val secretKey = factory.generateSecret(spec)

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val params = IvParameterSpec(iv)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, params)

    val decryptedBytes = cipher.doFinal(encryptedBytes)
    return String(decryptedBytes)
}

/**
 * Computes the SHA-256 hash of the string and encodes it as Base64.
 * @return The Base64-encoded hash.
 */
fun String.hash() = Base64.encodeToString(
    MessageDigest
        .getInstance("SHA-256")
        .digest(toByteArray()),
    Base64.NO_WRAP
)!!

/**
 * Checks if the hash of this string matches the given hash.
 * @param hash The hash to compare to.
 * @return True if the hashes match, false otherwise.
 */
inline fun String.checkHash(hash: String) = hash() == hash

/**
 * Waits until the [condition] returns true or the [timeout] is reached.
 * @param timeout The maximum time to wait in milliseconds (0 for no timeout).
 * @param condition The condition to check.
 * @return True if the condition was met, false if timed out.
 */
@Suppress("unused")
inline fun waitUntil(timeout: Long = 0, condition: () -> Boolean): Boolean {
    val time = System.currentTimeMillis()
    while (!condition()) {
        if (timeout > 0 && System.currentTimeMillis() - time > timeout) {
            return false
        }
    }
    return true
}

/**
 * Waits while the [condition] returns true or until the [timeout] is reached.
 * @param timeout The maximum time to wait in milliseconds (0 for no timeout).
 * @param condition The condition to check.
 * @return True if the condition became false, false if timed out.
 */
inline fun waitWhile(timeout: Long = 0, condition: () -> Boolean): Boolean {
    val time = System.currentTimeMillis()
    while (condition()) {
        if (timeout > 0 && System.currentTimeMillis() - time > timeout) {
            return false
        }
    }
    return true
}

/**
 * Creates a [SensorEventListener] for orientation sensors.
 * @param callback Called with azimuth, pitch, and roll values.
 * @return The [SensorEventListener].
 */
inline fun orientationSensorEventListener(
    crossinline callback: (Float, Float, Float) -> Unit
) = SensorEventListener { event ->
    val accelerometerData = event.values.takeIf { event.sensor.type == Sensor.TYPE_ACCELEROMETER }
    val geomagneticData = event.values.takeIf { event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD }

    if (accelerometerData != null && geomagneticData != null) {
        val R = FloatArray(9)
        if (
            SensorManager.getRotationMatrix(
                R,
                FloatArray(9),
                accelerometerData,
                geomagneticData
            )
        ) {
            val (azimuth, pitch, roll) = FloatArray(3).also { SensorManager.getOrientation(R, it) }
            callback(azimuth, pitch, roll)
        }
    }
}

/**
 * Creates a [SensorEventListener] for generic sensor events.
 * @param callback Called with the [SensorEvent].
 * @return The [SensorEventListener].
 */
inline fun SensorEventListener(crossinline callback: (SensorEvent) -> Unit) = object: SensorEventListener {
    override fun onSensorChanged(event: SensorEvent) {
        callback(event)
    }
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}

/**
 * Returns true if the device screen is currently locked.
 */
inline val Context.isScreenLocked get() = (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked

/**
 * Manages notification IDs, allowing reservation and release of IDs.
 * @param reservedIds IDs or ranges to reserve initially.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class NotificationIdManager(vararg reservedIds: Any) {
    private val reserved = reservedIds.flatMap {
        when (it) {
            is Int -> listOf(it)
            is String -> listOf(it.toInt())
            is IntRange -> it.toList()
            else -> throw IllegalArgumentException("Invalid type")
        }
    }.toMutableList()

    fun reserve(id: Int) = reserved.add(id)

    fun release(id: Int) = reserved.remove(id)

    fun get(): Int {
        while (true) {
            val random = Random.nextInt(0..Int.MAX_VALUE)
            if (!reserved.contains(random)) {
                return random
            }
        }
    }

    fun getAndReserve(): Int {
        val id = get()
        reserve(id)
        return id
    }
}

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

/**
 * Tests if an [InputStream] can be read and closed without exception.
 * @return True if successful, false otherwise.
 */
inline fun InputStream.test() = try {
    read()
    close()
    true
} catch (_: Exception) {
    false
}

/**
 * Tests if a [ContentResolver] can open and read a [Uri].
 * @param item The [Uri] to test.
 * @return True if successful, false otherwise.
 */
inline fun ContentResolver.test(item: Uri) =
    try {
        openInputStream(item)!!.test()
    } catch (_: Exception) {
        false
    }

/**
 * Gets a [Parcelable] extra from an [Intent], handling API differences.
 * @param key The extra key.
 * @return The [Parcelable] extra, or null if not found.
 */
inline fun <reified T: Parcelable> Intent.getParcelableExtraAs(
    key: String
) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getParcelableExtra(key, T::class.java)
} else {
    @Suppress("DEPRECATION")
    getParcelableExtra(key)
}

/**
 * Gets a [Serializable] extra from an [Intent], handling API differences.
 * @param key The extra key.
 * @return The [Serializable] extra, or null if not found.
 */
inline fun <reified T: Serializable> Intent.getSerializableExtraAs(
    key: String
) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getSerializableExtra(key, T::class.java)
} else {
    @Suppress("DEPRECATION")
    getSerializableExtra(key) as T
}

/**
 * Compares two [ActivityInfo] objects for content equality.
 * @param other The other [ActivityInfo] to compare.
 * @return True if the contents are equal, false otherwise.
 */
fun ActivityInfo.contentEquals(other: ActivityInfo): Boolean {
    return (
        Build.VERSION.SDK_INT < 26 || colorMode == other.colorMode &&
        Build.VERSION.SDK_INT < 34 || requiredDisplayCategory == other.requiredDisplayCategory &&
        theme == other.theme &&
        launchMode == other.launchMode &&
        documentLaunchMode == other.documentLaunchMode &&
        permission == other.permission &&
        taskAffinity == other.taskAffinity &&
        targetActivity == other.targetActivity &&
        flags == other.flags &&
        screenOrientation == other.screenOrientation &&
        configChanges == other.configChanges &&
        softInputMode == other.softInputMode &&
        uiOptions == other.uiOptions &&
        parentActivityName == other.parentActivityName &&
        maxRecents == other.maxRecents &&
        windowLayout == other.windowLayout
    )
}

/**
 * Gets a launch [Intent] for this [ActivityInfo].
 */
inline val ActivityInfo.launchIntent get() = Intent().apply {
    component = ComponentName(packageName!!, name)
    flags = FLAG_ACTIVITY_NEW_TASK
}

/**
 * Returns true if this [ActivityInfo] is a launcher activity.
 * @param context The context to use for lookup.
 * @return True if this is a launcher activity, false otherwise.
 */
fun ActivityInfo.isLauncher(context: Context): Boolean {
    with (context) {
        @Suppress("ExplicitThis")
        val intent = launchIntent.apply {
            action = ACTION_MAIN
            addCategory(CATEGORY_LAUNCHER)
            component = ComponentName(this@isLauncher.packageName!!, name)
            flags = FLAG_ACTIVITY_NEW_TASK
        }
        val infos = packageManager.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER)
        for (info in infos) {
            val filter = info.filter
            if (
                info.activityInfo.contentEquals(this@isLauncher) &&
                filter != null &&
                filter.hasAction(ACTION_MAIN) &&
                filter.hasCategory(CATEGORY_LAUNCHER)
            ) {
                return true
            }
        }
        return false
    }
}

/**
 * Encodes a string for safe use in JSON.
 * @return The JSON-encoded string.
 */
fun String.encodeJSON(): String {
    val out = StringBuilder()
    for (i in indices) {
        when (val c: Char = get(i)) {
            '"', '\\', '/' -> out.append('\\').append(c)
            '\t' -> out.append("\\t")
            '\b' -> out.append("\\b")
            '\n' -> out.append("\\n")
            '\r' -> out.append("\\r")
            else -> if (c.code <= 0x1F) {
                out.append(String.format("\\u%04x", c.code))
            } else {
                out.append(c)
            }
        }
    }
    return "$out"
}

/**
 * Allows [WeakReference] to be used as a delegated property.
 */
@Suppress("unused", "RedundantSuppression")
inline operator fun <T> WeakReference<T>.getValue(
    thisRef: Any,
    property: KProperty<*>
) = get()

/**
 * Runs a block and logs any exception, returning null on error.
 * @param tag The log tag.
 * @param message The log message.
 * @param block The block to run.
 * @return The result of the block, or null if an exception occurred.
 */
inline fun <T> runOrLog(
    tag: String,
    message: String = "An error occurred:",
    crossinline block: () -> T
) = try {
    block()
} catch (e: Exception) {
    Log.e(tag, message, e)
    null
}

typealias WidthSizeClass = WindowWidthSizeClass
typealias HeightSizeClass = WindowHeightSizeClass
typealias SizeClass = WindowSizeClass

/**
 * Posts a notification if the
 * [POST_NOTIFICATIONS][android.Manifest.permission.POST_NOTIFICATIONS]
 * permission is granted.
 * @param id The notification ID.
 * @param notification The notification to post.
 */
inline fun Context.notifyIfAllowed(
    id: Int,
    notification: Notification
) {
    with (NotificationManagerCompat.from(this)) {
        if (
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return@with
        }
        notify(id, notification)
    }
}

/**
 * Converts a [BaseBundle] to a [Map].
 * @return The map of keys to values.
 */
@Suppress("DEPRECATION")
fun BaseBundle.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    for (key in keySet()) {
        map[key] = get(key)!!
    }
    return map
}

/**
 * Converts a [Bundle] to a [PersistableBundle].
 * All non-persistable data will be lost in the created bundle.
 * 
 * @return The created [PersistableBundle].
 */
fun Bundle.toPersistableBundle(): PersistableBundle {
    val map = toMap()
    val bundle = PersistableBundle()
    for ((key, value) in map) {
        when (value) {
            is Int -> bundle.putInt(key, value)
            is Long -> bundle.putLong(key, value)
            is Double -> bundle.putDouble(key, value)
            is String -> bundle.putString(key, value)
            is IntArray -> bundle.putIntArray(key, value)
            is LongArray -> bundle.putLongArray(key, value)
            is DoubleArray -> bundle.putDoubleArray(key, value)
            is PersistableBundle -> bundle.putPersistableBundle(key, value)
            is Boolean -> bundle.putBoolean(key, value)
            is BooleanArray -> bundle.putBooleanArray(key, value)
            is Array<*> -> {
                if (value.isArrayOf<String>()) {
                    @Suppress("UNCHECKED_CAST")
                    bundle.putStringArray(key, value as Array<String>)
                }
            }
        }
    }
    return bundle
}

/**
 * Decodes a string from Windows-1251 encoding to UTF-8.
 * @return The decoded string.
 */
inline fun String.decodeWindows1251() = String(
    toByteArray(Charsets.ISO_8859_1),
    Charsets.UTF_8
)

/**
 * Configures the Ktor client for kotlinx.serialization JSON with custom settings.
 * @param settings Lambda to configure the [JsonBuilder].
 */
inline fun Configuration.json(crossinline settings: JsonBuilder.() -> Unit) {
    json(
        Json {
            settings()
        }
    )
}

/**
 * Installs [ContentNegotiation] with JSON support and custom settings in a Ktor client config.
 * @param settings Lambda to configure the [JsonBuilder].
 */
inline fun HttpClientConfig<*>.jsonConfig(crossinline settings: JsonBuilder.() -> Unit) {
    install(ContentNegotiation) {
        json {
            settings()
        }
    }
}

/**
 * Installs [DefaultRequest] in a Ktor client config.
 * @param settings Lambda to configure the [DefaultRequest.DefaultRequestBuilder].
 */
inline fun HttpClientConfig<*>.defaultRequest(
    crossinline settings: DefaultRequest.DefaultRequestBuilder.() -> Unit
) {
    install(DefaultRequest) {
        settings()
    }
}

/**
 * Installs [Logging] in a Ktor client config.
 * @param settings Lambda to configure the [LoggingConfig].
 */
inline fun HttpClientConfig<*>.logging(
    crossinline settings: LoggingConfig.() -> Unit
) {
    install(Logging) {
        settings()
    }
}

/**
 * Installs [Logging] in a Ktor client config with default settings (all logs).
 */
inline fun HttpClientConfig<*>.logging() = logging {
    logger = Logger.DEFAULT
    level = LogLevel.ALL
}

/**
 * Adds a vertical scroll modifier using [rememberScrollState].
 */
inline fun Modifier.verticalScroll() = composed {
    verticalScroll(rememberScrollState())
}

/**
 * Applies a modifier conditionally.
 * @param condition If true, applies [block] to this modifier.
 * @param block The modifier to apply.
 * @return The resulting [Modifier].
 */
inline fun Modifier.applyIf(
    condition: Boolean,
    block: (Modifier) -> Modifier
) = if (condition) this + block(this) else this

/**
 * Throws if the HTTP response status code is an error (>=400).
 * @return The [HttpResponse] if successful.
 * @throws ClientRequestException
 * @throws ServerResponseException
 * @throws ResponseException on error.
 */
suspend fun HttpResponse.failOnError(): HttpResponse {
    Log.d("failOnError", "Checking the response code...")
    val statusCode = status.value
    suspend fun body() = bodyAsText()

    Log.d("failOnError", "code: $statusCode")

    when (statusCode) {
        in 400..499 -> throw ClientRequestException(this, body())
        in 500..599 -> throw ServerResponseException(this, body())
    }

    if (statusCode >= 600) {
        throw ResponseException(this, body())
    }

    Log.d("failOnError", "Success")

    return this
}

/**
 * Evaluates JavaScript in a [WebView] without a callback.
 * @param script The JavaScript code to evaluate.
 */
inline fun WebView.evaluateJavascript(script: String) = evaluateJavascript(script, null)

/**
 * A [BroadcastReceiver] that registers for a specific action.
 * @param actions The actions to listen for.
 */
abstract class ActionedBroadcastReceiver(vararg val actions: String): BroadcastReceiver() {
    inline fun register(context: Context, exported: Boolean = false) {
        ContextCompat.registerReceiver(
            context,
            this,
            IntentFilter().apply {
                actions.forEach { addAction(it) }
            },
            if (exported)
                ContextCompat.RECEIVER_EXPORTED
            else ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    inline fun unregister(context: Context) = context.unregisterReceiver(this)
}

/**
 * Creates a [BroadcastReceiver] for a specific action.
 * @param actions The actions to listen for.
 * @param receiver The lambda to invoke on receive.
 * @return The created [BroadcastReceiver].
 */
inline fun broadcastReceiver(
    vararg actions: String,
    crossinline receiver: Context.(Intent) -> Unit
) = object: ActionedBroadcastReceiver(*actions) {
    override fun onReceive(context: Context, intent: Intent) {
        if (actions.isEmpty() || intent.action in actions) {
            receiver(context, intent)
        }
    }
}

/**
 * Creates a [PendingIntent] for a notification button broadcast.
 * @param action The action string for the intent.
 * @return The [PendingIntent].
 */
inline fun Context.notificationButtonPendingIntent(
    action: String
) = PendingIntent.getBroadcast(
    this,
    0,
    Intent(action).setPackage(packageName),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)!!

/**
 * Returns true if the [MediaPlayer] is playing, or false if not or if an exception occurs.
 */
inline val MediaPlayer.isPlayingSafe get() = runCatching { isPlaying }.getOrNull() == true


fun runMultiple(vararg instructions: () -> Unit): Boolean {
    var result = true

    instructions.forEach {
        if (
            runCatching {
                it()
            }.isFailure
        ) result = false
    }

    return result
}