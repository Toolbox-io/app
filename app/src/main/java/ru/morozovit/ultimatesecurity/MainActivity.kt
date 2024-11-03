package ru.morozovit.ultimatesecurity

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import ru.morozovit.ultimatesecurity.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= 31) {
            val splashScreen = installSplashScreen()
            splashScreen.setOnExitAnimationListener { splashScreenView ->
                // Create your custom animation.
                splashScreenView.view.animate()
                    .scaleX(3f)
                    .scaleY(3f)
                    .alpha(0f)
                    .setDuration(200L)
                    .setListener(object: AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {}
                        override fun onAnimationEnd(animation: Animator) {
                            splashScreenView.remove()
                        }
                        override fun onAnimationCancel(animation: Animator) {
                            splashScreenView.remove()
                        }
                        override fun onAnimationRepeat(animation: Animator) {}
                    })
                    .start()
            }
        } else {
            setTheme(R.style.Theme_UltimateSecurity_NoActionBar)
        }
        super.onCreate(savedInstanceState)
        Settings.init(applicationContext)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!UpdateCheckerService.running) {
            try {
                startService(
                    Intent(
                        this,
                        UpdateCheckerService::class.java
                    )
                )
            } catch (_: Exception) {}
        }

        setSupportActionBar(binding.toolbar)

        val drawerLayout = binding.drawerLayout
        val navView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_applocker, R.id.nav_unlock_protection, R.id.nav_settings
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(binding.root, R.string.grant_notification, Snackbar.LENGTH_LONG)
                    .setAction(R.string.grant) {
                        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101);
                    }
                    .show()
            }
        }
    }

    /* override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    } */

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}