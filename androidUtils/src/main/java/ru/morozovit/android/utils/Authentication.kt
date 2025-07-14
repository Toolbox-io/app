package ru.morozovit.android.utils

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Configuration for biometric authentication prompts.
 */
class AuthenticationConfig internal constructor() {
    internal var success: ((BiometricPrompt.AuthenticationResult) -> Unit)? = null
    internal var fail: (() -> Unit)? = null
    internal var error: ((Int, String) -> Unit)? = null
    internal var always: (() -> Unit)? = null

    /**
     * Set the callback that will be run when authentication
     * **succeeded**.
     *
     * @param block The callback to run
     */
    fun success(block: (BiometricPrompt.AuthenticationResult) -> Unit) {
        success = block
    }

    /**
     * Set the callback that will be run when authentication
     * **fails** (the user's identity couldn't be verified).
     *
     * @param block The callback to run
     */
    fun fail(block: () -> Unit) {
        fail = block
    }

    /**
     * Set the callback that will be run when an unrecoverable
     * error in authentication has occurred.
     *
     * @param block The callback to run. It has 2 parameters: the error code
     *              and the error string.
     */
    fun error(block: (errorCode: Int, errString: String) -> Unit) {
        error = block
    }

    /**
     * Set the callback that will run when authentication
     * either [succeeded][success] or [failed][fail].
     *
     * The callback will NOT run if there was an [unrecoverable error][error].
     *
     * @param block The callback to run.
     */
    fun always(block: () -> Unit) {
        always = block
    }

    /**
     * The title of the biometric prompt.
     *
     * This is a **required** property.
     */
    lateinit var title: String

    /**
     * The subtitle of the biometric prompt.
     */
    var subtitle: String? = null

    /**
     * The **close/authenticate with password** button text.
     *
     * This is a **required** property.
     */
    lateinit var negativeButtonText: String
}

/**
 * Requests biometric authentication in a [FragmentActivity] with a
 * [AuthenticationConfig].
 *
 * The [title][AuthenticationConfig.title] and
 * [negative button text][AuthenticationConfig.negativeButtonText] must
 * be set in the config.
 *
 * @param config Lambda to configure the authentication prompt and callbacks.
 * @return The [BiometricPrompt] instance.
 * @throws IllegalStateException if required fields are not set.
 */
fun FragmentActivity.requestAuthentication(
    config: AuthenticationConfig.() -> Unit
) = AuthenticationConfig().let {
    config(it)

    try {
        it.title
        it.negativeButtonText
    } catch (_: UninitializedPropertyAccessException) {
        throw IllegalStateException("Required fields haven't been set")
    }

    BiometricPrompt(
        this@requestAuthentication,
        ContextCompat.getMainExecutor(this@requestAuthentication),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                it.fail?.invoke()
                it.always?.invoke()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                it.success?.invoke(result)
                it.always?.invoke()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                it.error?.invoke(errorCode, "$errString")
            }
        }
    ).apply {
        authenticate(
            BiometricPrompt.PromptInfo.Builder().apply {
                setTitle(it.title)
                setSubtitle(it.subtitle)
                setNegativeButtonText(it.negativeButtonText)
                setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                setConfirmationRequired(false)
            }.build()
        )
    }
}