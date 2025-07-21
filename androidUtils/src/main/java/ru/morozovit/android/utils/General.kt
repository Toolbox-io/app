@file:Suppress("NOTHING_TO_INLINE")

package ru.morozovit.android.utils

import android.util.Log
import ru.morozovit.utils.toCamelCase

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

/**
 * Runs a block and logs any exception, returning `null` on error.
 * @param tag The log tag.
 * @param message The log message.
 * @param block The block to run.
 * @return The result of the block, or `null` if an exception occurred.
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
 * Throws [UnsupportedOperationException] when accessed.
 *
 * It is very useful for [Serializable] data classes that need
 * to have default values for params, but they must be specified.
 */
inline val unsupported: Nothing get() = throw UnsupportedOperationException()


/**
 * Capitalizes the first letter of a string if there's any.
 * If the string if empty, an empty string is returned.
 *
 * @return The string with the first letter capitalized.
 */
inline fun String.capitalizeFirstLetter() = replaceFirstChar { it.uppercase() }

/**
 * Converts the string to **pascal case**.
 *
 * Example:
 * `ExampleString`
 *
 * @return The string converted to pascal case.
 */
inline fun String.toPascalCase() = toCamelCase().capitalizeFirstLetter()