package ru.morozovit.ultimatesecurity.applocker

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.databinding.ApplockerPermissionsBinding
import ru.morozovit.ultimatesecurity.isAccessibilityPermissionAvailable

class PermissionsRequiredActivity: AppCompatActivity() {
    private lateinit var binding: ApplockerPermissionsBinding

    private var waitingForAccessibility = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ApplockerPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.alPrToolbar.setNavigationOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        if (isAccessibilityPermissionAvailable) {
            /* binding.alPrAB.setText(R.string.granted)
            binding.alPrAB.isEnabled = false */
            setResult(RESULT_OK)
            finish()
        }

        binding.alPrAB.setOnClickListener {
            if (isAccessibilityPermissionAvailable) {
                /* binding.alPrAB.setText(R.string.granted)
                binding.alPrAB.isEnabled = false */
                setResult(RESULT_OK)
                finish()
            } else {
                val intent = Intent(ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = FLAG_ACTIVITY_NEW_TASK
                waitingForAccessibility = true
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (waitingForAccessibility) {
            if (isAccessibilityPermissionAvailable) {
                /* binding.alPrAB.setText(R.string.granted)
                binding.alPrAB.isEnabled = false */
                setResult(RESULT_OK)
                finish()
            } else {
                Snackbar.make(binding.root, R.string.pr_wasnt_granted, Snackbar.LENGTH_SHORT).show()
            }
            waitingForAccessibility = false
        }
    }
}