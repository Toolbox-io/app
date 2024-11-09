package ru.morozovit.ultimatesecurity.applocker

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.result.ActivityResult
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import ru.morozovit.android.BetterActivityResult
import ru.morozovit.android.ui.makeSwitchCard
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.SelectAppsActivity
import ru.morozovit.ultimatesecurity.Service
import ru.morozovit.ultimatesecurity.Service.Companion.waitingForAccessibility
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.Settings.Applocker.getUnlockModeDescription
import ru.morozovit.ultimatesecurity.Settings.Applocker.unlockMode
import ru.morozovit.ultimatesecurity.Settings.accessibility
import ru.morozovit.ultimatesecurity.databinding.ApplockerBinding
import ru.morozovit.android.homeScreen
import java.lang.Thread.sleep


class ApplockerFragment : Fragment() {
    private lateinit var binding: ApplockerBinding
    private lateinit var activityLauncher: BetterActivityResult<Intent, ActivityResult>
    private var checkListener = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ApplockerBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO fix android 11 bug with accesibility service
        activityLauncher = BetterActivityResult.registerActivityForResult(this)
        with(binding) {
            makeSwitchCard(applockerSwitchCard, applockerSwitch)
            if (accessibility)
                applockerSwitch.isChecked = true

            applockerSwitch.setOnCheckedChangeListener { v, checked ->
                if (checkListener) {
                    if (checked) {
                        checkListener = false
                        v.isChecked = false
                        checkListener = true
                        MaterialAlertDialogBuilder(requireActivity())
                            .setTitle(R.string.permissions_required)
                            .setMessage(R.string.al_permissions)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.ok) { d, _ ->
                                d.dismiss()
                                val intent = Intent(ACTION_ACCESSIBILITY_SETTINGS)
                                intent.flags = FLAG_ACTIVITY_NEW_TASK
                                waitingForAccessibility = true
                                startActivity(intent)
                            }
                            .show()
                    } else {
                        var error = false
                        try {
                            Service.instance!!.disable()
                        } catch (e: Exception) {
                            error = true
                        }

                        if (accessibility || error) {
                            applockerSwitch.isChecked = true
                            Snackbar.make(
                                root,
                                R.string.error_disabling_service,
                                LENGTH_SHORT
                            )
                                .setAction(R.string.settings) {
                                    val intent = Intent(ACTION_ACCESSIBILITY_SETTINGS)
                                    intent.flags = FLAG_ACTIVITY_NEW_TASK
                                    startActivity(intent)
                                }
                                .show()
                        }
                    }
                }
            }

            binding.alTfc.setOnClickListener {
                homeScreen()

                sleep(500)

                requireActivity().finish()

                val intent = Intent(activity, FakeCrashActivity::class.java)
                val b = Bundle()
                b.putString("appPackage", requireActivity().packageName)
                startActivity(intent)
            }

            binding.alApps.setOnClickListener {
                val intent = Intent(requireActivity(), SelectAppsActivity::class.java)
                startActivity(intent)
            }

            binding.alTc.setOnClickListener {
                @Suppress("DIVISION_BY_ZERO")
                1 / 0 // Make an exception for the app to crash
            }

            binding.alPw.setOnClickListener {
                val intent = Intent(activity, PasswordInputActivity::class.java)
                val b = Bundle()
                b.putBoolean("setPassword", true)
                intent.putExtras(b)
                activityLauncher.launch(intent) { result ->
                    if (result.resultCode == RESULT_OK) {
                        try {
                            Settings.Applocker.password =
                                result.data?.extras?.getString("password")!!
                        } catch (e: NullPointerException) {
                            Toast.makeText(activity, R.string.failed_setting_password, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            binding.alUm.setOnClickListener {
                val intent = Intent(activity, UnlockModeActivity::class.java)
                activityLauncher.launch(intent) {
                    binding.alI5C.text = getUnlockModeDescription(unlockMode, resources)
                }
            }

            binding.alI5C.text = getUnlockModeDescription(unlockMode, resources)
        }
    }

    override fun onResume() {
        super.onResume()
        if (accessibility && waitingForAccessibility) {
            checkListener = false
            binding.applockerSwitch.isChecked = true
            checkListener = true
            waitingForAccessibility = false
        }
    }
}