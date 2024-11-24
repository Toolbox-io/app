package ru.morozovit.ultimatesecurity.ui.tools

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.jahnen.libaums.core.UsbMassStorageDevice
import ru.morozovit.android.ActivityLauncher
import ru.morozovit.android.BetterActivityResult.registerActivityForResult
import ru.morozovit.android.RadioButtonController
import ru.morozovit.android.getFileName
import ru.morozovit.android.getOpenDocumentIntent
import ru.morozovit.android.getSystemService
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.databinding.FlasherBinding
import ru.morozovit.utils.EParser
import java.io.IOException
import java.util.Arrays.copyOfRange
import kotlin.math.min


class FlasherFragment: Fragment() {
    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        const val BYTE = 1L
        const val KILOBYTE = BYTE * 1024L
        const val MEGABYTE = KILOBYTE * 1024L
        const val GIGABYTE = MEGABYTE * 1024L
        const val TERABYTE = GIGABYTE * 1024L

        private const val UNKNOWN_ERROR = -1
        private const val SUCCESS = 0
        private const val FILE_BIGGER_THAN_DEVICE = 1
        private const val FILE_BIGGER_THAN_DEVICE_0B = 4
        private const val NO_IMAGE_CHOSEN = 2
        private const val NO_DEVICE_SELECTED = 3

        const val ACTION_USB_PERMISSION = "FlasherFragment.ACTION_USB_PERMISSION"
    }

    private lateinit var binding: FlasherBinding
    private lateinit var activityLauncher: ActivityLauncher
    private lateinit var controller: RadioButtonController
    private lateinit var devices: Array<Device>
    private lateinit var usbManager: UsbManager

    private var img: Image? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FlasherBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n", "UnspecifiedRegisterReceiverFlag")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activityLauncher = registerActivityForResult(this)
        binding.flasherStart.shrink()
        val rbs = mutableListOf<RadioButton>()
        controller = RadioButtonController(rbs)
        devices = getUsbStorageDevices()
        usbManager = getSystemService(UsbManager::class)!!

        binding.flasherStart.setOnClickListener {
            val result = checkStartConditions()
            if (result != SUCCESS) {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.flash_error)
                    .setMessage("${resources.getString(R.string.flash_error_d)}\n${decodeReason(result)}")
                    .setPositiveButton(R.string.ok, null)
                    .show()
            } else {
                val dev = devices[controller.checkedIndex].data.usbDevice
                val permissionIntent =
                    PendingIntent.getBroadcast(requireActivity(), 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)
                val filter = IntentFilter(ACTION_USB_PERMISSION)
                val usbReceiver = object: BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (ACTION_USB_PERMISSION == intent.action) {
                            synchronized(this) {
                                val device: UsbDevice? = if (Build.VERSION.SDK_INT >= 33) {
                                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                                } else {
                                    @Suppress("DEPRECATION")
                                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                                }

                                try {
                                    if (intent.getBooleanExtra(
                                            UsbManager.EXTRA_PERMISSION_GRANTED,
                                            false
                                        )
                                    ) {
                                        device!!.apply {
                                            val result2 = checkStartConditions2()
                                            if (result2 == SUCCESS) {
                                                fun findInterface(): UsbInterface? {
                                                    for (nIf in 0 until device.interfaceCount) {
                                                        val usbInterface: UsbInterface =
                                                            device.getInterface(nIf)
                                                        if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                                                            return usbInterface
                                                        }
                                                    }
                                                    return null
                                                }

                                                val mUsbInterface = findInterface()
                                                if (mUsbInterface != null) {
                                                    var mOutEndpoint: UsbEndpoint? = null
                                                    var mInEndpoint: UsbEndpoint? = null

                                                    for (nEp in 0 until mUsbInterface.endpointCount) {
                                                        val tmpEndpoint: UsbEndpoint =
                                                            mUsbInterface.getEndpoint(nEp)
                                                        if (tmpEndpoint.type != UsbConstants.USB_ENDPOINT_XFER_BULK) continue

                                                        if ((mOutEndpoint == null)
                                                            && (tmpEndpoint.direction == UsbConstants.USB_DIR_OUT)
                                                        ) {
                                                            mOutEndpoint = tmpEndpoint
                                                        } else if ((mInEndpoint == null)
                                                            && (tmpEndpoint.direction == UsbConstants.USB_DIR_IN)
                                                        ) {
                                                            mInEndpoint = tmpEndpoint
                                                        }
                                                    }
                                                    if (mOutEndpoint == null) throw IOException("No write endpoint: $deviceName")
                                                    val mConnection = usbManager.openDevice(device)
                                                    fun write(
                                                        data: Array<Byte>,
                                                        length: Int,
                                                        offset1: Int = 0
                                                    ): Int {
                                                        var offset = offset1
                                                        assert(length != 0)

                                                        while (offset < length) {
                                                            val size = min(
                                                                length - offset,
                                                                mInEndpoint!!.maxPacketSize
                                                            )
                                                            val bytesWritten =
                                                                mConnection.bulkTransfer(
                                                                    mOutEndpoint,
                                                                    copyOfRange(
                                                                        data.toByteArray(),
                                                                        offset,
                                                                        (offset + size)
                                                                    ),
                                                                    size,
                                                                    0
                                                                )
                                                            Log.d(
                                                                "Flasher",
                                                                "BytesWritten: $bytesWritten"
                                                            )
                                                            if (bytesWritten < 0) throw IOException(
                                                                "None written"
                                                            )
                                                            offset += bytesWritten
                                                        }
                                                        return offset;
                                                    }

                                                    if (mConnection != null) {
                                                        mConnection.claimInterface(
                                                            mUsbInterface,
                                                            true
                                                        )

                                                        val byteArr =
                                                            "test".toByteArray().toTypedArray()
                                                        write(byteArr, byteArr.size)
                                                    } else {
                                                        Log.wtf("Flasher", "Connection failed!")
                                                    }
                                                } else {
                                                    Log.wtf("Flasher", "Usb interface not found!")
                                                }
                                            } else {
                                                MaterialAlertDialogBuilder(requireActivity())
                                                    .setTitle(R.string.flash_error)
                                                    .setMessage(
                                                        "${resources.getString(R.string.flash_error_d)}\n${
                                                            decodeReason(
                                                                result
                                                            )
                                                        }"
                                                    )
                                                    .setPositiveButton(R.string.ok, null)
                                                    .show()
                                            }
                                        }
                                    } else {
                                        Log.w("Flasher", "permission denied for device $device")
                                    }
                                } catch (e: Exception) {
                                    Log.e("Flasher", "Error while flashing: ${EParser(e)}")
                                }
                            }
                        }
                    }
                }

                requireActivity().apply {
                    if (Build.VERSION.SDK_INT >= 33) {
                        registerReceiver(
                            usbReceiver,
                            filter,
                            Context.RECEIVER_EXPORTED
                        )
                    } else {
                        registerReceiver(usbReceiver, filter)
                    }
                }
                usbManager.requestPermission(dev, permissionIntent)
            }
        }

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

        controller.setOnSelectListener { _, _ ->
            checkStartConditions()
        }

        binding.flasherImageChoose.setOnClickListener {
            activityLauncher.launch(
                getOpenDocumentIntent(
                    "application/vnd.efi.img",
                    "application/x-iso9660-image"
                )
            ) {
                if (it.resultCode == Activity.RESULT_OK) {
                    it.data!!.data!!.let { uri ->
                        img = Image(
                            getFileName(uri),
                            uri,
                            requireContext().contentResolver.openFileDescriptor(uri,
                                "r")?.let { th ->
                                val result = th.statSize
                                th.close()
                                result
                            } ?: 0
                        )
                        Log.d("Flasher", "$img")
                        binding.flasherImageName.text = img!!.name
                        binding.flasherImageSize.text = img!!.sizeString
                        binding.flasherImage.visibility = VISIBLE
                        binding.flasherImageChoose.updateLayoutParams<MarginLayoutParams> {
                            setMargins(0)
                        }
                        checkStartConditions()
                    }
                } else {
                    Log.w("Flasher", "Failed. Result = ${it.resultCode}")
                }
            }
        }
    }

    private fun checkStartConditions(): Int {
        val image = img

        if (image != null && controller.checkedItem != null) {
            binding.flasherStart.extend()
            return SUCCESS
        } else {
            binding.flasherStart.shrink()
        }
        if (image == null) {
            return NO_IMAGE_CHOSEN
        } else if (controller.checkedItem == null) {
            return NO_DEVICE_SELECTED
        }
        return UNKNOWN_ERROR
    }

    private fun checkStartConditions2(): Int {
        var storageSize = 0L
        if (controller.checkedIndex != -1) {
            val device = devices[controller.checkedIndex].data
            for (part in device.partitions) {
                val fs = part.fileSystem
                storageSize += fs.capacity
            }
        } else {
            storageSize = -1
        }

        val image = img
        if (image != null && image.size > storageSize && storageSize != -1L) {
            binding.flasherImageSize.setTextColor(Color.RED)
            return if (storageSize == 0L) FILE_BIGGER_THAN_DEVICE_0B else FILE_BIGGER_THAN_DEVICE
        }

        if (image != null && controller.checkedItem != null && image.size <= storageSize) {
            binding.flasherStart.extend()
            return SUCCESS
        } else {
            binding.flasherStart.shrink()
        }
        if (image == null) {
            return NO_IMAGE_CHOSEN
        } else if (controller.checkedItem == null) {
            return NO_DEVICE_SELECTED
        } else if (image.size > storageSize) {
            return FILE_BIGGER_THAN_DEVICE
        }
        return UNKNOWN_ERROR
    }

    data class Device(val name: String, val path: String, val data: UsbMassStorageDevice)
    data class Image(val name: String, val path: Uri, val size: Long) {
        val sizeString = "Approximately " +
            if (size >= TERABYTE) {
                "${size / TERABYTE} TB"
            } else if (size >= GIGABYTE) {
                "${size / GIGABYTE} GB"
            } else if (size >= MEGABYTE) {
                "${size / MEGABYTE} MB"
            } else if (size >= KILOBYTE) {
                "${size / KILOBYTE} KB"
            } else {
                "$size B"
            }
    }

    private fun decodeReason(errorCode: Int): String {
        return when (errorCode) {
            NO_IMAGE_CHOSEN -> resources.getString(R.string.no_image_chosen)
            NO_DEVICE_SELECTED -> resources.getString(R.string.no_device_selected)
            FILE_BIGGER_THAN_DEVICE -> resources.getString(R.string.file_bigger_than_device)
            FILE_BIGGER_THAN_DEVICE_0B -> resources.getString(R.string.file_bigger_than_device_0b)
            else -> resources.getString(R.string.unknown_error)
        }
    }

    private fun getUsbStorageDevices(): Array<Device> {
        val deviceList = UsbMassStorageDevice.getMassStorageDevices(requireActivity())
        val storageDevices = mutableListOf<Device>()

        for (device in deviceList) {
            val deviceName = device.usbDevice.productName ?: "<no name>"
            val devicePath = device.usbDevice.deviceName
            storageDevices.add(Device(deviceName, devicePath, device))
        }

        return storageDevices.toTypedArray()
    }
}