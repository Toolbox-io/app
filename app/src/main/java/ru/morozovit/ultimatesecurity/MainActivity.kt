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
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import ru.morozovit.ultimatesecurity.App.Companion.authenticated
import ru.morozovit.ultimatesecurity.Settings.globalPassword
import ru.morozovit.ultimatesecurity.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    lateinit var lockView: View

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
        menu.findItem(R.id.lock).apply {
            isVisible = globalPassword != ""
        }
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

    override fun finish() {
        super.finish()
        authenticated = false
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}