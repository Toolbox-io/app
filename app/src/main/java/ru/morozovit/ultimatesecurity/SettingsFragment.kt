package ru.morozovit.ultimatesecurity

import android.app.Activity.RESULT_OK
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.morozovit.android.BetterActivityResult
import ru.morozovit.android.ui.makeSwitchCard
import ru.morozovit.ultimatesecurity.databinding.SettingsBinding


class SettingsFragment : Fragment() {
    private lateinit var binding: SettingsBinding
    private lateinit var activityLauncher: BetterActivityResult<Intent, ActivityResult>
    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponentName: ComponentName

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)
        activityLauncher = BetterActivityResult.registerActivityForResult(this)
        dpm = requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponentName = ComponentName(requireActivity(), DeviceAdmin::class.java)
        makeSwitchCard(binding.sDeviceadmin, binding.sI1Sw)

        binding.sI1Sw.isChecked = dpm.isAdminActive(adminComponentName)

        var listener = true

        binding.sI1Sw.setOnCheckedChangeListener { v, isChecked ->
            if (listener) {
                if (isChecked) {
                    v.isChecked = false
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName)
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
                        listener = false
                        v.isChecked = result.resultCode == RESULT_OK
                        listener = true
                    }
                } else {
                    dpm.removeActiveAdmin(adminComponentName)
                    Settings.UnlockProtection.enabled = false
                }
            }
        }

        binding.sDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.delete_app)
                .setMessage(R.string.delete_app_d)
                .setNeutralButton(R.string.cancel, null)
                .setPositiveButton(R.string.yes) { _, _ ->
                    binding.sI1Sw.isChecked = false
                    val intent = Intent(
                        Intent.ACTION_DELETE, Uri.fromParts(
                            "package",
                            requireActivity().packageName, null
                        )
                    )
                    startActivity(intent)
                }
                .show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {}
}