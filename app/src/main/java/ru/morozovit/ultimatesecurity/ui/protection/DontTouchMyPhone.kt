package ru.morozovit.ultimatesecurity.ui.protection

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.morozovit.android.SensorEventListener
import ru.morozovit.android.copy
import ru.morozovit.android.invoke
import ru.morozovit.android.orientationSensorEventListener
import ru.morozovit.android.ui.Category
import ru.morozovit.android.ui.CategoryDefaults
import ru.morozovit.android.ui.ListItem
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.ui.WindowInsetsHandler
import ru.morozovit.utils.EParser
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DontTouchMyPhoneScreen(@Suppress("LocalVariableName") EdgeToEdgeBar: @Composable (@Composable (PaddingValues) -> Unit) -> Unit) {
    WindowInsetsHandler {
        EdgeToEdgeBar { innerPadding ->
            Column(Modifier.padding(innerPadding)) {
                val context = LocalContext()
                val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
                val mediaPlayer = remember { MediaPlayer() }

                var touched by remember { mutableStateOf(false) }
                var started by remember { mutableStateOf(false) }
                var accelerometerListener: SensorEventListener?
                var orientationListener: SensorEventListener?

                var currentRoll by remember { mutableFloatStateOf(0f) }
                var currentPitch by remember { mutableFloatStateOf(0f) }

                DisposableEffect(Unit) {
                    var lastData = Pair(0f, 0f)

                    accelerometerListener = SensorEventListener { event ->
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]

                        // Simple threshold for touch detection
                        if ((x > 5 || y > 5 || z > 20) && started) {
                            touched = true
                        }
                    }
                    orientationListener = orientationSensorEventListener { _, pitch, roll ->
                        currentPitch = (pitch * 100f).roundToInt() / 100.0f
                        currentRoll = (roll * 100f).roundToInt() / 100.0f

                        val prevPitch = lastData.first
                        val prevRoll = lastData.second

                        val pitchTest = abs(abs(prevPitch) - abs(currentPitch))
                        val rollTest = abs(abs(prevRoll) - abs(currentRoll))

                        if (
                            (
                                    pitchTest >= 0.01 ||
                                            rollTest >= 0.01
                                    ) && started
                        ) {
                            touched = true
                        }

                        lastData = Pair(currentPitch, currentRoll)
                    }

                    fun tryRegister(sensor: Sensor?, listener: SensorEventListener) {
                        try {
                            sensor!!
                            sensorManager.registerListener(
                                listener,
                                sensor,
                                SensorManager.SENSOR_DELAY_NORMAL
                            )
                        } catch (e: Exception) {
                            Log.e("DontTouchMyPhone", "Sensor $sensor cannot be registered: ${EParser(e)}")
                        }
                    }

                    tryRegister(
                        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        accelerometerListener!!
                    )

                    tryRegister(
                        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        orientationListener!!
                    )
                    tryRegister(
                        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                        orientationListener!!
                    )

                    onDispose {
                        sensorManager?.unregisterListener(accelerometerListener)
                        sensorManager?.unregisterListener(orientationListener)
                        mediaPlayer.stop()
                        mediaPlayer.release()
                    }
                }

                LaunchedEffect(Unit) {
                    snapshotFlow { touched }.collect {
                        if (it) {
                            started = false
                            touched = false
                            Settings.Actions.run(
                                context,
                                mediaPlayer,
                                context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            )
                        }
                    }
                }

                Category(margin = CategoryDefaults.margin.copy(top = 16.dp)) {
                    ListItem(
                        headline = stringResource(R.string.actions),
                        supportingText = stringResource(R.string.actions_d),
                        onClick = {
                            context.startActivity(
                                Intent(
                                    context,
                                    ActionsActivity::class.java
                                )
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null
                            )
                        }
                    )
                }

                Button(
                    onClick = {
                        touched = false
                        started = !started
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        if (started)
                            stringResource(R.string.stop)
                        else
                            stringResource(R.string.start)
                    )
                }
            }
        }
    }
}