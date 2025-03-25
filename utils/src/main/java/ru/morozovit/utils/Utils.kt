@file:Suppress("unused", "NOTHING_TO_INLINE")

package ru.morozovit.utils

import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

fun String.shorten(chars: Int) =
    if (length <= chars)
        this
    else
        substring(0, chars - 3) + "..."

object MarkdownHeaderParser {
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

fun String.toCamelCase(): String {
    val words = this.split(" ", "_", "-")
    val camelCase = StringBuilder()
    for ((index, word) in words.withIndex()) {
        camelCase.append(if (index == 0) word.lowercase() else word.replaceFirstChar { it.uppercase() })
    }
    return "$camelCase"
}

fun <T> MutableList<T>.add(element: T?): Boolean {
    return if (element == null) false else add(element)
}

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

fun File.safeDelete() = try {
    delete()
} catch (e: Exception) {
    false
}

fun Collection<File>.safeDelete(): Boolean {
    if (isNullOrEmpty()) {
        return false
    }
    var result = true
    for (file in this) {
        if (!file.safeDelete()) {
            result = false
        }
    }
    return result
}