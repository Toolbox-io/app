package ru.morozovit.android.utils

import kotlin.random.Random
import kotlin.random.nextInt

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
            Random.Default.nextInt(0..Int.MAX_VALUE).let {
                if (!reserved.contains(it)) return it
            }
        }
    }

    fun getAndReserve() = get().also { reserve(it) }
}