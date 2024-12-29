package ru.morozovit.android

import java.util.concurrent.Executor

@Suppress("MemberVisibilityCanBePrivate")
class NoParallelExecutor: Executor {
    val isRunning get() = thread != null
    var thread: Thread? = null

    override fun execute(command: Runnable) {
        if (!isRunning) {
            thread = async {
                command.run()
                thread = null
            }
        }
    }
}