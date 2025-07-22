@file:Suppress("NOTHING_TO_INLINE")

package ru.morozovit.android.utils

import kotlinx.serialization.Serializable

@Serializable
data class SemanticVersion(
    val major: Int = 0,
    val minor: Int = 0,
    val patch: Int = 0
): java.io.Serializable {
    override fun equals(other: Any?) = other != null && (
        (
            other is SemanticVersion &&
            major == other.major &&
            minor == other.minor &&
            patch == other.patch
        ) || (
            other is Int &&
            other == toString().toInt()
        ) || (
            other.toString() == toString()
        )
    )

    operator fun compareTo(other: SemanticVersion) =
        major
            .compareTo(other.major)
            .change { maj ->
                minor
                    .compareTo(other.minor)
                    .change { min ->
                        patch
                            .compareTo(other.patch)
                            .takeIf { min == 0 }
                    }
                    .takeIf { maj == 0 }
            }

    override fun toString() = buildString {
        if (major > 0) append(major)
        if (minor > 0) append(".$minor")
        if (patch > 0) append(".$patch")
    }

    override fun hashCode(): Int {
        var result = major.hashCode()
        result = 31 * result + minor.hashCode()
        result = 31 * result + patch.hashCode()
        return result
    }
}

inline fun SemanticVersion(string: String?) = if (string.isNullOrBlank()) {
    SemanticVersion()
} else {
    val (maj, min, pat) = string
        .split(".")
        .map { it.toInt() }
        .fillTo(3) { 0 }
    SemanticVersion(maj, min, pat)
}

inline fun String.toSemanticVersion() = SemanticVersion(this)
inline fun Int.toSemanticVersion() = SemanticVersion(this)
inline fun Float.toSemanticVersion() = SemanticVersion(toString())
inline fun Double.toSemanticVersion() = SemanticVersion(toString())