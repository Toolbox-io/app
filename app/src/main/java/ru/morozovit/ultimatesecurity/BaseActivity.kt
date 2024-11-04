package ru.morozovit.ultimatesecurity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // transparentStatusBar()
        Settings.init(applicationContext)
        super.onCreate(savedInstanceState)
    }
}