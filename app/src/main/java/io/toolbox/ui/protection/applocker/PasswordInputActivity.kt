@file:Suppress("NOTHING_TO_INLINE")

package io.toolbox.ui.protection.applocker

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.postDelayed
import io.toolbox.R
import io.toolbox.Settings
import io.toolbox.services.Accessibility
import io.toolbox.ui.OverlayAppTheme
import ru.morozovit.android.utils.ui.ComposeView
import ru.morozovit.android.utils.screenWidth
import ru.morozovit.android.utils.ui.SecureTextField

class PasswordInputActivity: AppCompatActivity() {
    companion object {
        inline fun start(context: Context, appPackage: String) {
            context.startActivity(
                Intent(context, PasswordInputActivity::class.java).apply {
                    addFlags(
                        FLAG_ACTIVITY_NEW_TASK or
                        FLAG_ACTIVITY_NEW_DOCUMENT or
                        FLAG_ACTIVITY_MULTIPLE_TASK
                    )
                    putExtra("appPackage", appPackage)
                }
            )
        }
    }

    @Composable
    inline fun PasswordInputScreen() {
        OverlayAppTheme(Modifier.fillMaxSize()) {
            var password by remember { mutableStateOf("") }
            var hidden by remember { mutableStateOf(true) }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 30.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .widthIn(max = 500.dp),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.enter_password),
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                        SecureTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.password)) },
                            hidden = hidden,
                            onHiddenChange = { hidden = it },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                autoCorrectEnabled = false
                            )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    setResult(RESULT_CANCELED)
                                    finish()
                                }
                            ) {
                                Text(stringResource(id = R.string.cancel))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    if (Settings.Keys.Applocker.check(password)) {
                                        setResult(RESULT_OK, intent)
                                        finish()
                                        Accessibility.instance?.lock = true

                                        try {
                                            val launchIntent = applicationContext
                                                .packageManager
                                                .getLaunchIntentForPackage(packageName)
                                            startActivity(launchIntent)
                                        } catch (e: Exception) {
                                            Log.e("PasswordInputActivity", "Couldn't launch activity: ", e)
                                        }

                                        Handler(mainLooper).postDelayed(2000) {
                                            Accessibility.instance?.lock = false
                                        }
                                    } else {
                                        Toast.makeText(
                                            this@PasswordInputActivity,
                                            resources.getString(R.string.wrong_password),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            ) {
                                Text(stringResource(id = R.string.ok))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            ComposeView {
                PasswordInputScreen()
            },
            ViewGroup.LayoutParams(
                screenWidth,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    override fun onPause() {
        super.onPause()
        setResult(RESULT_CANCELED)
        finish()
    }
}