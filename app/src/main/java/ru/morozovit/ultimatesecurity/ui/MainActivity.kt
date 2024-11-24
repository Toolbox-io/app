package ru.morozovit.ultimatesecurity.ui

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import ru.morozovit.android.setNegativeButton
import ru.morozovit.ultimatesecurity.App.Companion.authenticated
import ru.morozovit.ultimatesecurity.BaseActivity
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings.exitDsa
import ru.morozovit.ultimatesecurity.Settings.globalPassword
import ru.morozovit.ultimatesecurity.Settings.globalPasswordEnabled
import ru.morozovit.ultimatesecurity.databinding.ActivityMainBinding
import ru.morozovit.ultimatesecurity.services.UpdateChecker
import ru.morozovit.ultimatesecurity.ui.AuthActivity.Companion.started

class MainActivity : BaseActivity(
    backButtonBehavior = Companion.BackButtonBehavior.DEFAULT,
) {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start update checker
        try {
            UpdateChecker.schedule(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "${e::class.qualifiedName}: ${e.message}")
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (pendingAuth) {
            binding.root.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (started) {
                        binding.root.viewTreeObserver.removeOnPreDrawListener(this)
                    }
                    return false
                }
            })
        }

        // Navigation
        setSupportActionBar(binding.toolbar)
        navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_settings,
                R.id.nav_website,
                R.id.nav_unlock_protection,
                R.id.nav_applocker,
                R.id.nav_tiles,
                R.id.nav_shortcuts,
                R.id.nav_flasher,
                R.id.nav_apkextractor
            ), binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        navController.navigate(intent.getIntExtra("nav", R.id.nav_home))

        // Notifications
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Snackbar.make(
                    binding.rootView,
                    R.string.grant_notification,
                    Snackbar.LENGTH_LONG
                )
                    .setAction(R.string.grant) {
                        requestPermissions(
                            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                            101
                        )
                    }
                    .show()
            }
        }

        if (!pendingAuth) startEnterAnimation(binding.root)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        updateLock()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.lock -> {
                authenticated = false
                auth()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        updateLock(menu)
        return true
    }

    override fun finish() {
        if (!exitDsa) {
            MaterialAlertDialogBuilder(this)
                .setMessage(R.string.exit)
                .setNeutralButton(R.string.dsa) { _, _ ->
                    exitDsa = true
                    super.finish()
                    authenticated = false
                }
                .setNegativeButton(R.string.no)
                .setPositiveButton(R.string.yes) { _, _ ->
                    super.finish()
                    authenticated = false
                }
                .show()
        } else {
            super.finish()
            authenticated = false
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (binding.navView.menu[0].isChecked) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp() =
        navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()


    override fun onResume() {
        super.onResume()
        if (!pendingAuth) updateLock()
    }

    fun updateLock(
        menu: Menu = binding.toolbar.menu
    ) {
        menu.findItem(R.id.lock)?.apply {
            isVisible = globalPassword != "" && globalPasswordEnabled
        }
        interactionDetector()
    }

    override fun onDestroy() {
        super.onDestroy()
        authenticated = false
        splashScreenDisplayed = false
        isSplashScreenVisible = true
    }
}