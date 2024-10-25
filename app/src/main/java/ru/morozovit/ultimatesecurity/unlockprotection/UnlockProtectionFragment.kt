package ru.morozovit.ultimatesecurity.unlockprotection

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.ActivityResult
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.morozovit.android.BetterActivityResult
import ru.morozovit.android.ui.makeSwitchCard
import ru.morozovit.ultimatesecurity.DeviceAdmin
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.Settings.UnlockProtection.enabled
import ru.morozovit.ultimatesecurity.databinding.UnlockProtectionBinding

class UnlockProtectionFragment : Fragment() {
    private lateinit var binding: UnlockProtectionBinding
    private lateinit var activityLauncher: BetterActivityResult<Intent, ActivityResult>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = UnlockProtectionBinding.inflate(inflater, container, false)
        savedInstanceState?.clear()
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)
        savedInstanceState?.clear()
        activityLauncher = BetterActivityResult.registerActivityForResult(this)
        val dpm = requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admComponent = ComponentName(requireActivity(), DeviceAdmin::class.java)
        makeSwitchCard(binding.unlockProtSwitchCard, binding.unlockProtSwitch)

        // Settings
        val unlockAttempts = binding.upUa
        val unlockAttemptsToErase = binding.upUaE
        val unlockAttemptsToEraseSwitch = binding.upI3Sw

        unlockAttempts.setText("${Settings.UnlockProtection.unlockAttempts}")
        unlockAttemptsToErase.setText("${Settings.UnlockProtection.unlockAttemptsToErase}")

        unlockAttemptsToEraseSwitch.setOnCheckedChangeListener { _, isChecked ->
            Settings.UnlockProtection.erase = isChecked
            unlockAttemptsToErase.isEnabled = isChecked
        }
        unlockAttemptsToEraseSwitch.isChecked = Settings.UnlockProtection.erase
        unlockAttemptsToErase.isEnabled = unlockAttemptsToEraseSwitch.isChecked

        // Switch
        var checkListener = false

        if (!dpm.isAdminActive(admComponent)) enabled = false
        binding.unlockProtSwitch.isChecked = if (!dpm.isAdminActive(admComponent)) false else enabled

        binding.unlockProtSwitch.setOnCheckedChangeListener { v, isChecked ->
            if (checkListener) {
                if (isChecked) {
                    if (dpm.isAdminActive(admComponent)) {
                        enabled = true
                        return@setOnCheckedChangeListener
                    }
                    checkListener = false
                    v.isChecked = false
                    checkListener = true
                    MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(R.string.permissions_required)
                        .setMessage(R.string.up_permissions)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok) { d, _ ->
                            d.dismiss()
                            val intent =
                                Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                    putExtra(
                                        DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                        admComponent
                                    )
                                    putExtra(
                                        DevicePolicyManager.EXTRA_ADD_EXPLANATION, """
                            This permission is needed for the following features:
                            - Protect this app from being removed by an intruder
                            - Detect failed unlock attempts to take the required actions
                            This app NEVER uses this permission for anything not listed above.
                            """.trimIndent()
                                    )
                                }
                            activityLauncher.launch(intent) { result ->
                                checkListener = false
                                v.isChecked = result.resultCode == RESULT_OK
                                checkListener = true
                                enabled = true
                            }
                        }
                        .show()
                } else {
                    enabled = false
                }
            }
        }

        checkListener = true
    }

    private fun save() {
        val unlockAttempts = binding.upUa
        val unlockAttemptsToErase = binding.upUaE

        Settings.UnlockProtection.unlockAttempts = unlockAttempts.toInt()
        Settings.UnlockProtection.unlockAttemptsToErase = unlockAttemptsToErase.toInt()
    }

    override fun onPause() {
        super.onPause()
        save()
    }

    override fun onDestroy() {
        super.onDestroy()
        save()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.clear()
    }
}

fun Editable?.toInt() = toString().toInt()
fun EditText.toInt() = text.toInt()
