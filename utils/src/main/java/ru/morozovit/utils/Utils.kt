@file:Suppress("unused", "NOTHING_TO_INLINE")

package ru.morozovit.utils

import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Shortens the string to the specified number of characters, appending '...' if truncated.
 *
 * @param chars The maximum number of characters to keep (including the ellipsis if truncated).
 * @return The shortened string, or the original string if its length is less than or equal to [chars].
 */
fun String.shorten(chars: Int) =
    if (length <= chars)
        this
    else
        substring(0, chars - 3) + "..."

/**
 * Utility object for parsing headers from Markdown text.
 *
 * The header is expected to be delimited by three dashes (---) at the start and end.
 * Each header line should be in the format: `key: value`.
 */
object MarkdownHeaderParser {
    /**
     * Finds the header section in the given markdown string.
     *
     * @param markdown The markdown text to search.
     * @return The header string if found, or null otherwise.
     */
    private fun findHeader(markdown: String): String? {
        var header = ""
        var started = false
        var secondTime = false
        var chrs = 0
        for (i in markdown.indices) {
            val chr = markdown[i]

            if (secondTime) {
                break
            }

            if (chrs == 3) {
                chrs = 0
                started = true
            }

            if (chr == '-') {
                chrs++
                if (started) {
                    started = false
                    secondTime = true
                }
            }

            if (started) {
                header += chr
            }
        }
        return header.ifEmpty { null }?.trim()
    }

    /**
     * Parses the header section of a markdown string into a map.
     *
     * @param markdown The markdown text containing a header.
     * @return A map of header keys to their values (as Int, Float, or String), or null if no header is found.
     */
    fun parseHeader(markdown: String): Map<String, Any>? {
        val header = findHeader(markdown) ?: return null

        val regex = "([\\w_-]+):\\s*(.*)"
        val pattern: Pattern = Pattern.compile(regex, Pattern.MULTILINE)
        val matcher: Matcher = pattern.matcher(header)

        val result = mutableMapOf<String, Any>()
        var currentKey: String? = null

        while (matcher.find()) {
            for (i in 1..matcher.groupCount()) {
                runCatching {
                    val group = matcher.group(i)
                    when (i) {
                        1 -> {
                            currentKey = group
                        }
                        2 -> {
                            when {
                                group.toIntOrNull() != null -> result[currentKey!!] = group.toInt()
                                group.toFloatOrNull() != null -> result[currentKey!!] = group.toFloat()
                                else -> result[currentKey!!] = group
                            }
                        }
                    }
                }
            }
        }

        return result
    }
}

/**
 * Converts a string to camelCase.
 *
 * Splits the string by spaces, underscores, or hyphens, then capitalizes each word except the first.
 *
 * @return The camelCase version of the string.
 */
fun String.toCamelCase(): String {
    val words = this.split(" ", "_", "-")
    val camelCase = StringBuilder()
    for ((index, word) in words.withIndex()) {
        camelCase.append(if (index == 0) word.lowercase() else word.replaceFirstChar { it.uppercase() })
    }
    return "$camelCase"
}

/**
 * Adds a non-null element to a mutable list.
 *
 * @param element The element to add.
 * @return True if the element was added, false if it was null.
 */
fun <T> MutableList<T>.add(element: T?): Boolean {
    return if (element == null) false else add(element)
}

/**
 * Deletes all files in the collection.
 *
 * @return True if all files were deleted successfully, false otherwise.
 */
fun Collection<File>.delete(): Boolean {
    if (isNullOrEmpty()) {
        return false
    }
    var result = true
    for (file in this) {
        if (!file.delete()) {
            result = false
        }
    }
    return result
}

/**
 * Attempts to delete the file, suppressing any exceptions.
 *
 * @return True if the file was deleted successfully, false otherwise.
 */
fun File.safeDelete() = try {
    delete()
} catch (e: Exception) {
    false
}

/**
 * Attempts to delete all files in the collection, suppressing any exceptions.
 *
 * @return True if all files were deleted successfully, false otherwise.
 */
fun Collection<File>.safeDelete(failFast: Boolean = false): Boolean {
    if (isNullOrEmpty()) {
        return false
    }
    var result = true
    for (file in this) {
        if (!file.safeDelete()) {
            result = false
            if (failFast) return false
        }
    }
    return result
}