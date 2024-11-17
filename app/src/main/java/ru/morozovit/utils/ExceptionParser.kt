package ru.morozovit.utils

import java.io.PrintWriter
import java.io.StringWriter

class ExceptionParser(val exception: Throwable) {
    companion object {
        fun eToString(e: Throwable) = "${ExceptionParser(e)}"
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

    override fun toString(): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        exception.printStackTrace(pw)
        return "$sw"
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
}