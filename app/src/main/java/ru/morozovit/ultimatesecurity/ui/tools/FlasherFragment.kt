package ru.morozovit.ultimatesecurity.ui.tools

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import ru.morozovit.android.ActivityLauncher
import ru.morozovit.android.BetterActivityResult.registerActivityForResult
import ru.morozovit.android.RadioButtonController
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.databinding.FlasherBinding

class FlasherFragment: Fragment() {
    private lateinit var binding: FlasherBinding
    private lateinit var activityLauncher: ActivityLauncher
    private var selectedFileUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FlasherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activityLauncher = registerActivityForResult(this)
        val rbs = mutableListOf<RadioButton>()
        val controller = RadioButtonController(rbs)
        val devices = getUsbStorageDevices()

        for (device in devices) {
            val devLayout = layoutInflater.inflate(
                R.layout.usb_device,
                binding.flasherDevices,
                true
            )
            val deviceName = devLayout.findViewById<TextView>(R.id.flasher_device_name)
            val devicePath = devLayout.findViewById<TextView>(R.id.flasher_device_path)
            val rb = devLayout.findViewById<RadioButton>(R.id.flasher_device_rb)
            deviceName.text = device.name
            devicePath.text = device.path
            controller.add(rb)
            devLayout.setOnClickListener {
                controller.select(rb)
            }
        }

        binding.flasherImageChoose.setOnClickListener {
            activityLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }) {
                if (it.resultCode == Activity.RESULT_OK) {
                    it.data!!.data!!.let { uri ->
                        selectedFileUri = uri
                        val fileName = uri.lastPathSegment
                        val fileSize = requireContext().contentResolver.openFileDescriptor(uri,
                            "r")?.let { th ->
                            val result = th.statSize
                            th.close()
                            result
                        } ?: 0
                        Log.d("Flasher", "$fileName, $fileSize")
                    }
                }
            }
        }
    }

    data class UsbDevice(val name: String, val path: String)

    private fun getUsbStorageDevices(): Array<UsbDevice> {
        val usbManager = requireActivity().getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = usbManager.deviceList
        val storageDevices = mutableListOf<UsbDevice>()

        for (device in deviceList.values) {
            for (i in 0 until device.interfaceCount) {
                val usbInterface = device.getInterface(i)
                if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE) {
                    val deviceName = device.productName ?: "<no name>"
                    val devicePath = device.deviceName
                    storageDevices.add(UsbDevice(deviceName, devicePath))
                }
            }
        }

        return storageDevices.toTypedArray()
    }
}