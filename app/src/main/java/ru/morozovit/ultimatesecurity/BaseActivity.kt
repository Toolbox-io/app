package ru.morozovit.ultimatesecurity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity(protected var authEnabled: Boolean = true): AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (authEnabled) auth()
    }

    protected fun auth(): Boolean {
        startActivity(Intent(this, AuthActivity::class.java))
        return authEnabled
    }
}