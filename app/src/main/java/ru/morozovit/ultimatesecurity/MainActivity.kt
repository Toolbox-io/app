package ru.morozovit.ultimatesecurity

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import ru.morozovit.android.setNegativeButton
import ru.morozovit.ultimatesecurity.App.Companion.authenticated
import ru.morozovit.ultimatesecurity.Settings.exitDsa
import ru.morozovit.ultimatesecurity.Settings.globalPassword
import ru.morozovit.ultimatesecurity.Settings.globalPasswordEnabled
import ru.morozovit.ultimatesecurity.databinding.ActivityMainBinding

class MainActivity : BaseActivity(
    backButtonBehavior = Companion.BackButtonBehavior.DEFAULT,
) {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    lateinit var lockView: View
    private lateinit var menu: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            UpdateChecker.schedule(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "${e::class.qualifiedName}: ${e.message}")
        }


        setSupportActionBar(binding.toolbar)

        val drawerLayout = binding.drawerLayout
        val navView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_applocker, R.id.nav_unlock_protection, R.id.nav_settings, R.id.nav_website
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(binding.rootView, R.string.grant_notification, Snackbar.LENGTH_LONG)
                    .setAction(R.string.grant) {
                        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101);
                    }
                    .show()
            }
        }

        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val th = this
                findViewById<ActionMenuItemView>(R.id.lock).apply {
                    if (this@apply != null) {
                        lockView = this
                        transitionName = "lock"

                        window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(th)
                    }
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        this.menu = menu
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

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()
        updateLock()
    }

    fun updateLock(
        menu: Menu = binding.toolbar.menu
    ) {
        menu.findItem(R.id.lock)?.apply {
            isVisible = globalPassword != "" && globalPasswordEnabled
        }
        interactionDetector()
    }
}