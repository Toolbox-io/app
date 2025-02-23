package ru.morozovit.utils

import java.util.regex.Matcher
import java.util.regex.Pattern

fun String.shorten(chars: Int) =
    if (length <= chars)
        this
    else
        substring(0, chars - 3) + "..."

object MarkdownHeaderParser {
    fun findHeader(markdown: String): String? {
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