package ru.morozovit.android

import java.util.concurrent.Executor
import kotlin.concurrent.thread

@Suppress("MemberVisibilityCanBePrivate")
class NoParallelExecutor: Executor {
    val isRunning get() = thread != null
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