@file:Suppress("NOTHING_TO_INLINE")

package ru.morozovit.utils

inline fun assertTrue(value: Boolean) {
    if (!value) {
        throw AssertionError("Assertion failed")
    }
}

inline fun assertFalse(value: Boolean) {
    if (value) {
        throw AssertionError("Assertion failed")
    }
}

inline fun assertNull(value: Any?) {
    if (value != null) {
        throw AssertionError("Expected null, but was: $value")
    }
}

inline fun Boolean?.assertTrue() {
    if (this == null || !this) {
        throw AssertionError("Assertion failed")
    }
}

inline fun Boolean?.assertFalse() {
    if (this == null || this) {
        throw AssertionError("Expected false, but was: $this")
    }
}

inline fun Any?.assertNull() {
    if (this != null) {
        throw AssertionError("Expected null, but was: $this")
    }
}