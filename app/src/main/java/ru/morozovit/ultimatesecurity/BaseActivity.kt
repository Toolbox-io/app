package ru.morozovit.ultimatesecurity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import ru.morozovit.ultimatesecurity.App.Companion.authenticated
import ru.morozovit.ultimatesecurity.BaseActivity.InteractionDetector.execute
import ru.morozovit.ultimatesecurity.Settings.globalPassword
import java.lang.Thread.sleep
import java.util.concurrent.Executor

abstract class BaseActivity(protected val authEnabled: Boolean = true): AppCompatActivity() {
    private var paused = false
    private var interacted = false

    private object InteractionDetector: Executor {
        private var executing = false

        override fun execute(command: Runnable) {
            if (!executing) {
                executing = true
                Thread {
                    command.run()
                    executing = false
                }.start()
            }
        }
    }

    private fun interactionDetector() {
        if (authEnabled && globalPassword != "") {
            execute {
                var timer = 0
                while (true) {
                    var br = false
                    for (i in 0..1000) {
                        sleep(1)
                        if (paused) {
                            br = true
                            break
                        }
                    }
                    if (br) break
                    if (interacted) {
                        interacted = false
                        timer = 0
                        Log.i("Auth", "Interaction detected, resetting")
                    } else {
                        timer++
                        Log.i("Auth", "No interaction, timer = $timer")
                    }

                    if (timer >= 60) {
                        authenticated = false
                        auth()
                        break
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        preSplashScreen()
        super.onCreate(savedInstanceState)
        if (authEnabled && globalPassword != "") {
            auth()
            interactionDetector()
        }
    }

    protected fun auth(): Boolean {
        if (authEnabled && globalPassword != "" && !authenticated) {
            startActivity(Intent(this, AuthActivity::class.java))
        }
        return authEnabled
    }

    override fun onResume() {
        super.onResume()
        paused = false
        interactionDetector()
    }

    override fun onPause() {
        super.onPause()
        paused = true
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        interacted = true
    }
}