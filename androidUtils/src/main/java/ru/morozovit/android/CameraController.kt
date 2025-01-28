package ru.morozovit.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Size
import androidx.core.content.ContextCompat
import androidx.core.os.postDelayed
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Collections
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class CameraController(val context: Context) {
    private var mCameraId: String? = null
    private var mCaptureSession: CameraCaptureSession? = null
    private var mCameraDevice: CameraDevice? = null

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var imageReader: ImageReader? = null
    private var file: Any? = null

    private val mCameraOpenCloseLock = Semaphore(1)

    private var shouldClose = false
    private var recursionLimit = 5

    var waitingForImage = false
        private set

    private val mStateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release()
            mCameraDevice = cameraDevice
            createCameraCaptureSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
        }
    }
    private val mOnImageAvailableListener =
        OnImageAvailableListener { reader ->
            Log.d(TAG, "ImageAvailable")
            backgroundHandler!!.post(when (file) {
                is File -> ImageSaver(reader.acquireNextImage(), file = file as File)
                is DocumentFile -> ImageSaver(reader.acquireNextImage(), documentFile = file as DocumentFile)
                else -> throw IllegalStateException()
            })
            waitingForImage = false
        }

    fun open(): Boolean {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        setUpCameraOutputs()
        val manager =
            context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            startBackgroundThread()
            manager.openCamera(mCameraId!!, mStateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            return false
        } catch (e: InterruptedException) {
            return false
        }
        return true
    }

    private fun setUpCameraOutputs() {
        val manager =
            context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // We don't use a front facing camera in this sample.
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    continue
                }

                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?: continue

                // For still image captures, we use the largest available size.
                val largest = Collections.max(
                    listOf(*map.getOutputSizes(ImageFormat.JPEG)),
                    CompareSizesByArea()
                )
                imageReader = ImageReader.newInstance(
                    largest.width,
                    largest.height,
                    ImageFormat.JPEG,  /*maxImages*/
                    2
                )
                imageReader!!.setOnImageAvailableListener(
                    mOnImageAvailableListener,
                    backgroundHandler
                )

                mCameraId = cameraId
                return
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            e.printStackTrace()
        }
    }

    fun close() {
        shouldClose = true
    }

    private fun closeCamera() {
        shouldClose = false
        try {
            mCameraOpenCloseLock.acquire()
            if (null != mCaptureSession) {
                mCaptureSession!!.close()
                mCaptureSession = null
            }
            if (null != mCameraDevice) {
                mCameraDevice!!.close()
                mCameraDevice = null
            }
            if (null != imageReader) {
                imageReader!!.close()
                imageReader = null
            }
            mCameraId = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            mCameraOpenCloseLock.release()
            stopBackgroundThread()
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun createCameraCaptureSession() {
        try {
            // Here, we create a CameraCaptureSession for camera preview.

            @Suppress("DEPRECATION")
            mCameraDevice!!.createCaptureSession(
                listOf(
                    imageReader!!.surface
                ),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        // The camera is already closed
                        if (null == mCameraDevice) {
                            return
                        }

                        // When the session is ready, we start displaying the preview.
                        mCaptureSession = cameraCaptureSession
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        Log.d(TAG, "Configuration Failed")
                    }

                    override fun onClosed(session: CameraCaptureSession) {
                        if (mCaptureSession != null && mCaptureSession == session) {
                            mCaptureSession = null
                        }
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun takePicture(filename: String): Boolean? {
        if (recursionLimit < 0) {
            recursionLimit = 5
            throw IllegalStateException()
        }
        if (waitingForImage) {
            return false
        }
        file = getFrontFile(filename)
        try {
            if (null == mCameraDevice) {
                return false
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            val captureBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReader!!.surface)

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )

            val captureCallback: CaptureCallback = object : CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    Log.d(TAG, file.toString())
                }
            }

            if (mCaptureSession != null) {
                mCaptureSession!!.stopRepeating()
                waitingForImage = true
                mCaptureSession!!.capture(captureBuilder.build(), captureCallback, null)
                recursionLimit = 5
                return true
            } else {
                Handler(Looper.getMainLooper()).postDelayed(20) {
                    recursionLimit--
                    takePicture(filename)
                }
                return null
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            return false
        }
    }

    private fun getFrontFile(filename: String): Any? {
        val mediaStorageDir = File(context.filesDir.absolutePath + "/front")
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }
        return File("${mediaStorageDir.absolutePath}/$filename.jpg")
    }

    private inner class ImageSaver(
        private val image: Image,
        private val file: File? = null,
        private val documentFile: DocumentFile? = null
    ) : Runnable {
        override fun run() {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer[bytes]
            var output: OutputStream? = null
            try {
                output =
                    if (file != null)
                        FileOutputStream(file)
                    else
                        context
                            .contentResolver
                            .openOutputStream(
                                documentFile!!.uri
                            )!!
                output.write(bytes)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                image.close()
                if (null != output) {
                    try {
                        output.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                if (shouldClose) {
                    closeCamera()
                }
                waitingForImage = false
            }
        }
    }

    private class CompareSizesByArea : Comparator<Size> {
        override fun compare(lhs: Size, rhs: Size): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
        }
    }

    companion object {
        private const val TAG = "CameraController"
    }
}