package ru.morozovit.ultimatesecurity.ui.tools

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ru.morozovit.android.invoke
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.ui.WindowInsetsHandler

@Composable
fun DontTouchMyPhoneScreen(EdgeToEdgeBar: @Composable (@Composable () -> Unit) -> Unit) {
    WindowInsetsHandler {
        EdgeToEdgeBar {
            val context = LocalContext()
            val sensorManager = context.getSystemService(SensorManager::class.java)
            val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            var touched by remember { mutableStateOf(false) }
            var sensorEventListener: SensorEventListener? = null

            LaunchedEffect(Unit) {
                sensorEventListener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        event?.let {
                            val x = event.values[0]
                            val y = event.values[1]
                            val z = event.values[2]

                            // Simple threshold for touch detection
                            if (x > 5 || y > 5 || z > 20) {
                                touched = true
                            }
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }

                accelerometerSensor?.also { sensor ->
                    sensorManager.registerListener(
                        sensorEventListener,
                        sensor,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    sensorEventListener?.let {
                        sensorManager?.unregisterListener(it)
                    }
                }
            }

            Button(
                onClick = {
                    // Reset the "touched" variable
                    touched = false
                }
            ) {
                Text(stringResource(R.string.start))
            }
            if (touched) {
                Text(stringResource(R.string.touched))
            }
        }
    }
}