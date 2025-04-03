@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package ru.morozovit.utils

import java.io.PrintWriter
import java.io.StringWriter
import java.util.Collections
import java.util.IdentityHashMap

typealias ExcParser = ExceptionParser
typealias EParser = ExceptionParser

class ExceptionParser(val exception: Throwable) {
    companion object {
        fun eToString(e: Throwable) = "${ExceptionParser(e)}"

        @JvmStatic
        infix fun string(e: Throwable) = eToString(e)
    }

    val message get() = exception.message ?: ""

    val stackTraceString get() = exception
        .stackTrace
        .joinToString("\n") {
            "${it.className}.${it.methodName}(${it.lineNumber})"
        }
    val stackTrace: Array<StackTraceElement> get() = exception.stackTrace

    val cause get() = exception.cause
    val causeParser get() = cause?.let { ExceptionParser(it) }

    private fun Throwable.fallbackToString(writer: PrintWriter) {
        val visitedExceptions: MutableSet<Throwable> = Collections.newSetFromMap(IdentityHashMap())
        visitedExceptions.add(this)
        writer.println(this)
        var size = stackTrace.size
        var counter = 0
        while (counter < size) {
            val traceElement = stackTrace[counter]
            writer.println("\tat $traceElement")
            ++counter
        }

        size = suppressed.size

        counter = 0
        while (counter < size) {
            val suppressedException = suppressed[counter]
            suppressedException.printEnclosedStackTrace(
                writer,
                stackTrace,
                "Suppressed: ",
                "\t",
                visitedExceptions
            )
            ++counter
        }

        this.cause?.printEnclosedStackTrace(
            writer,
            stackTrace,
            "Caused by: ",
            "",
            visitedExceptions
        )
    }

    private fun Throwable.printEnclosedStackTrace(
        writer: PrintWriter,
        enclosingTrace: Array<StackTraceElement>,
        caption: String,
        prefix: String,
        visitedExceptions: MutableSet<Throwable>
    ) {
        try {
            if (visitedExceptions.contains(this)) {
                writer.println("$prefix$caption[CIRCULAR REFERENCE: $this]")
            } else {
                visitedExceptions.add(this)
                val trace: Array<StackTraceElement> = this.stackTrace
                var m = trace.size - 1

                var n = enclosingTrace.size - 1
                while (m >= 0 && n >= 0 && trace[m] == enclosingTrace[n]) {
                    --m
                    --n
                }

                val framesInCommon = trace.size - 1 - m
                writer.println(prefix + caption + this)

                for (i in 0..m) {
                    writer.println("${prefix}\tat ${trace[i]}")
                }

                if (framesInCommon != 0) {
                    writer.println("$prefix\t... $framesInCommon more")
                }

                for (element in suppressed) {
                    element.printEnclosedStackTrace(
                        writer,
                        trace,
                        "Suppressed: ",
                        prefix + "\t",
                        visitedExceptions
                    )
                }

                this.cause?.printEnclosedStackTrace(
                    writer,
                    trace,
                    "Caused by: ",
                    prefix,
                    visitedExceptions
                )
            }
        } catch (e: Exception) {
            runCatching {
                e.printStackTrace()
            }
            writer.println("$prefix$caption[Exception ocurred while printing stack trace]")
        }
    }

    override fun toString(): String {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            exception.printStackTrace(pw)
            return "$sw"
        } catch (e: Exception) {
            runCatching {
                e.printStackTrace()
            }
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                exception.fallbackToString(pw)
                return "$sw"
            } catch (e: Exception) {
                var exceptionStr: String? = null
                runCatching {
                    exceptionStr = "${EParser(e)}"
                }
                return if (exceptionStr != null) {
                    """
                        |Cannot show exception details, because another exception occurred.
                        |
                        |$exceptionStr
                    """.trimMargin()
                } else {
                    """
                        Cannot show exception details, because another exception occurred.
                    """.trimIndent()
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Throwable -> {
                (message == other.message && stackTrace.contentEquals(other
                    .stackTrace) && (
                        (cause == null && other.cause == null) ||
                                ExceptionParser(cause!!).equals(other.cause)
                        )
                        )
            }
            is ExceptionParser -> {
                equals(other.exception)
            }
            else -> {
                false
            }
        }
    }

    override fun hashCode(): Int {
        var result = exception.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + stackTraceString.hashCode()
        result = 31 * result + stackTrace.contentHashCode()
        result = 31 * result + (cause?.hashCode() ?: 0)
        result = 31 * result + (causeParser?.hashCode() ?: 0)
        return result
    }

    fun thr(): Nothing = throw exception
}

fun Exception.asString() = "${EParser(this)}"
fun Exception.asStr() = asString()