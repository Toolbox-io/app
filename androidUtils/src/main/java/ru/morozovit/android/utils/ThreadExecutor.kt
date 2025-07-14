package ru.morozovit.android.utils

import java.util.concurrent.Executor
import kotlin.concurrent.thread

@Suppress("MemberVisibilityCanBePrivate")
class ThreadExecutor: Executor {
    inline val isRunning get() = thread != null
    var thread: Thread? = null

    override fun execute(command: Runnable) {
        if (!isRunning) {
            thread = thread {
                command.run()
                thread = null
            }
        }
    }
}