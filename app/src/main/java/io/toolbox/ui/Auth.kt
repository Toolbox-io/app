@file:Suppress("NOTHING_TO_INLINE")

package io.toolbox.ui

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Handler
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResult
import androidx.annotation.AnimRes
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.postDelayed
import io.toolbox.App.Companion.authenticated
import io.toolbox.BaseActivity
import io.toolbox.R
import io.toolbox.Settings
import io.toolbox.services.Accessibility
import io.toolbox.ui.protection.applocker.ApplockerAuthOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.morozovit.android.ActivityLauncher
import ru.morozovit.android.BetterActivityResult
import ru.morozovit.android.homeScreen
import ru.morozovit.android.invoke
import ru.morozovit.android.requestAuthentication
import ru.morozovit.utils.EParser
import kotlin.concurrent.thread

open class Auth(private val context: Context, var intent: Intent = Intent()) {
    companion object {
        const val PASSWORD_DOT = "●"
        const val MAX_PASSWORD_LENGTH = 6

        const val MODE_ENTER = 0
        const val MODE_SET = 1
        const val MODE_CONFIRM = 2
        const val MODE_ENTER_OLD_PW = 3

        var started = false
    }

    var mode
        inline get() = intent.getIntExtra("mode", MODE_ENTER)
        inline set(value) { intent.putExtra("mode", value) }
    val isSetOrConfirm inline get() = mode in 1..3
    val enteredPassword inline get() =
        intent.getStringExtra("password") ?:
        throw NullPointerException("Password must be set")
    val oldPwConfirmed inline get() = intent.getBooleanExtra("oldPwConfirmed", false)
    val setStarted inline get() = intent.getBooleanExtra("setStarted", false)
    val applocker inline get() = intent.getBooleanExtra("applocker", false)

    val key inline get() =
        if (applocker)
            Settings.Keys.Applocker
        else Settings.Keys.App


    lateinit var activityLauncher: ActivityLauncher
    lateinit var launchingIntent: Intent

    private var blur = mutableStateOf(false)

    class PasswordEntry(
        val symbol: Int,
        visible: Boolean = false
    ) {
        var visible by mutableStateOf(visible)

        operator fun component1() = symbol
        operator fun component2() = visible

        override fun toString() = "PasswordEntry(symbol = $symbol, visible = $  visible)"
    }

    private class Handlers {
        lateinit var k0: () -> Unit
        lateinit var k1: () -> Unit
        lateinit var k2: () -> Unit
        lateinit var k3: () -> Unit
        lateinit var k4: () -> Unit
        lateinit var k5: () -> Unit
        lateinit var k6: () -> Unit
        lateinit var k7: () -> Unit
        lateinit var k8: () -> Unit
        lateinit var k9: () -> Unit
        lateinit var kBackspace: () -> Unit

        operator fun get(key: Key) = when (key) {
            Key.Zero -> k0
            Key.One -> k1
            Key.Two -> k2
            Key.Three -> k3
            Key.Four -> k4
            Key.Five -> k5
            Key.Six -> k6
            Key.Seven -> k7
            Key.Eight -> k8
            Key.Nine -> k9
            Key.Backspace -> kBackspace
            else -> throw IllegalStateException()
        }

        fun addNumberHandler(number: Int, handler: () -> Unit) {
            when (number) {
                0 -> k0 = handler
                1 -> k1 = handler
                2 -> k2 = handler
                3 -> k3 = handler
                4 -> k4 = handler
                5 -> k5 = handler
                6 -> k6 = handler
                7 -> k7 = handler
                8 -> k8 = handler
                9 -> k9 = handler
                else -> throw IllegalArgumentException("Number must be between 0 and 9")
            }
        }
    }

    @Composable
    private inline fun DetectAppTheme(noinline content: @Composable () -> Unit) {
        if (context is BaseActivity && !applocker) {
            AppTheme {
                content()
            }
        } else {
            OverlayAppTheme(content = content)
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    fun AuthScreen(mode: Int) {
        with (context) {
            DetectAppTheme {
                val handlers = remember { Handlers() }

                if (!isSetOrConfirm && context is BaseActivity) BackHandler(onBack = ::homeScreen)

                Scaffold(
                    topBar = {
                        if (isSetOrConfirm) {
                            TopAppBar(
                                title = {},
                                navigationIcon = {
                                    IconButton(
                                        onClick = {
                                            if (context is BaseActivity) {
                                                context.onBackPressedDispatcher.onBackPressed()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.back)
                                        )
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .onKeyEvent {
                            if (
                                it.type == KeyEventType.KeyUp
                            ) {
                                try {
                                    handlers[it.key]()
                                    return@onKeyEvent true
                                } catch (e: Exception) {
                                    Log.d("Auth", it.key.toString())
                                    Log.e("Auth", "${EParser(e)}")
                                }
                            }
                            false
                        }
                        .requiredHeight(
                            (LocalWindowInfo().containerSize.height - (60 * LocalDensity().density)).dp
                        ),
                    containerColor = Color.Transparent
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainer,
                                RoundedCornerShape(32.dp)
                            )
                            .fillMaxHeight()
                    ) {
                        val content = @Composable {
                            val coroutineScope = rememberCoroutineScope()
                            val view = LocalView()

                            val symbols = remember { mutableStateListOf<PasswordEntry>() }

                            var offsetSet by remember { mutableIntStateOf(0) }
                            val offset by animateIntOffsetAsState(
                                targetValue = IntOffset(offsetSet, 0),
                                animationSpec = tween(durationMillis = 70),
                                label = "wrong"
                            )
                            var triggerVisibility by remember { mutableStateOf(false) }
                            var inputLocked by remember { mutableStateOf(false) }

                            var currentPasswordState by remember { mutableIntStateOf(0) }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = stringResource(R.string.locked),
                                    modifier = Modifier.size(50.dp)
                                )
                                Text(
                                    text = stringResource(
                                        when (mode) {
                                            MODE_ENTER -> R.string.enter_password
                                            MODE_SET ->
                                                if (!key.isSet)
                                                    R.string.setpassword
                                                else
                                                    R.string.change_password

                                            MODE_ENTER_OLD_PW -> R.string.enter_old_password
                                            MODE_CONFIRM -> R.string.confirm_password
                                            else -> R.string.empty
                                        }
                                    ),
                                    fontSize = 25.sp,
                                    modifier = Modifier.padding(top = 20.dp)
                                )

                                Crossfade(
                                    targetState = currentPasswordState,
                                    modifier = Modifier
                                        .padding(top = 10.dp, bottom = 20.dp)
                                        .let {
                                            if (currentPasswordState == 1)
                                                it.animateContentSize()
                                            else
                                                it
                                        },
                                    label = ""
                                ) {
                                    when (it) {
                                        0 -> Row(
                                            modifier = Modifier
                                                .animateContentSize()
                                                .offset { offset },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Spacer(Modifier.width(20.dp))

                                            SideEffect {
                                                if (triggerVisibility) {
                                                    triggerVisibility = false
                                                    symbols.last().visible = true
                                                }
                                            }

                                            symbols.forEach { symbol ->
                                                AnimatedVisibility(
                                                    visible = symbol.visible,
                                                    enter = scaleIn() + fadeIn(),
                                                    exit = scaleOut() + fadeOut()
                                                ) {
                                                    Text(
                                                        text = PASSWORD_DOT,
                                                        fontSize = 30.sp
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.width(20.dp))
                                        }

                                        1 -> CircularProgressIndicator(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .align(Alignment.CenterHorizontally)
                                        )
                                    }
                                }
                            }

                            fun clear() {
                                coroutineScope.launch {
                                    symbols.forEach {
                                        it.visible = false
                                    }
                                    delay(500)
                                    symbols.clear()
                                }
                            }

                            fun processPassword() {
                                thread {
                                    inputLocked = true
                                    currentPasswordState = 1
                                    val password = symbols.let {
                                        var string = ""
                                        symbols.forEach {
                                            string += it.symbol
                                        }
                                        string
                                    }

                                    when {
                                        !isSetOrConfirm && key.check(password) -> {
                                            if (applocker) {
                                                Accessibility.instance?.lock = true
                                                Handler(mainLooper).postDelayed(2000) {
                                                    Accessibility.instance?.lock = false
                                                }
                                            } else {
                                                authenticated = true
                                            }
                                            finishAfterTransition(R.anim.scale_down, R.anim.alpha_down)
                                        }

                                        mode == MODE_ENTER_OLD_PW && key.check(password) -> {
                                            view.post {
                                                startActivity(Intent(this@with, AuthActivity::class.java).apply {
                                                    putExtra("mode", MODE_SET)
                                                    putExtra("oldPwConfirmed", true)
                                                    putExtra("noAnim", true)
                                                    putExtra("applocker", applocker)
                                                })
                                                finish()
                                            }
                                        }

                                        mode == MODE_SET -> {
                                            view.post {
                                                activityLauncher.launch(Intent(this@with, AuthActivity::class.java).apply {
                                                    putExtra("mode", MODE_CONFIRM)
                                                    putExtra("password", password)
                                                    putExtra("noAnim", true)
                                                    putExtra("applocker", applocker)
                                                }) {
                                                    if (it.resultCode == RESULT_OK) {
                                                        setResult(RESULT_OK)
                                                        finish()
                                                    }
                                                }
                                            }
                                            clear()
                                        }

                                        mode == MODE_CONFIRM && password == enteredPassword -> {
                                            key.set(password)
                                            view.post {
                                                setResult(RESULT_OK)
                                                finish()
                                            }
                                        }

                                        else -> {
                                            currentPasswordState = 0
                                            val delay = 100L

                                            offsetSet = -5
                                            Thread.sleep(delay)
                                            offsetSet = 10
                                            Thread.sleep(delay)
                                            offsetSet = -15
                                            Thread.sleep(delay)
                                            offsetSet = 20
                                            Thread.sleep(delay)
                                            offsetSet = -15
                                            Thread.sleep(delay)
                                            offsetSet = 10
                                            Thread.sleep(delay)
                                            offsetSet = -5
                                            Thread.sleep(delay)
                                            offsetSet = 0
                                            Thread.sleep(delay)
                                            clear()
                                            Thread.sleep(500)
                                        }
                                    }

                                    inputLocked = false
                                }
                            }

                            FlowRow(
                                maxItemsInEachRow = 3,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                @Composable
                                fun NumberButton(number: Int) {
                                    Button(
                                        shape = CircleShape,
                                        onClick = {
                                            Log.i("applocker", "button $number pressed, inputLocked: $inputLocked")
                                            if (!inputLocked) {
                                                Log.i("applocker", "adding char")
                                                val entry = PasswordEntry(number, false)
                                                symbols += entry
                                                triggerVisibility = true
                                                if (symbols.size == MAX_PASSWORD_LENGTH) processPassword()
                                            }
                                        }.also { handlers.addNumberHandler(number, it) },
                                        modifier = Modifier.size(75.dp),
                                        contentPadding = PaddingValues()
                                    ) {
                                        Text(
                                            text = "$number",
                                            fontSize = 35.sp
                                        )
                                    }
                                }

                                repeat(9) {
                                    NumberButton(it + 1)
                                }

                                var resetCount by remember { mutableIntStateOf(0) }
                                val lock = remember { Any() }

                                FilledIconButton(
                                    shape = CircleShape,
                                    modifier = Modifier.size(75.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color(0xFFF44336),
                                        contentColor = Color.White
                                    ),
                                    onClick = {
                                        if (!inputLocked) {
                                            coroutineScope.launch {
                                                try {
                                                    val reset: Int
                                                    synchronized(lock) {
                                                        resetCount++
                                                        reset = resetCount
                                                        symbols[symbols.lastIndex - reset + 1].visible = false
                                                    }
                                                    delay(500)
                                                    synchronized(lock) {
                                                        symbols.removeAt(symbols.lastIndex - reset + 1)
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("Auth", "${EParser(e)}")
                                                    Log.d("Auth", "$symbols")
                                                    delay(250)
                                                    val toRemove = mutableListOf<PasswordEntry>()

                                                    var vis = true
                                                    @Suppress("NAME_SHADOWING")
                                                    for (it in symbols) {
                                                        if (!it.visible) {
                                                            toRemove += it
                                                        } else {
                                                            vis = false
                                                            break
                                                        }
                                                    }

                                                    if (vis) {
                                                        toRemove.forEach {
                                                            symbols.remove(it)
                                                        }
                                                    }
                                                } finally {
                                                    resetCount--
                                                }
                                            }
                                        }
                                        Unit
                                    }.also { handlers.kBackspace = it }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = stringResource(R.string.erase),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                NumberButton(0)

                                FilledIconButton(
                                    shape = CircleShape,
                                    modifier = Modifier.size(75.dp),
                                    onClick = ::requestAuth,
                                    enabled =
                                        if (!isSetOrConfirm) {
                                            BiometricManager.from(context)
                                                .canAuthenticate(
                                                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                                                ) == BIOMETRIC_SUCCESS && Settings.allowBiometric
                                        } else {
                                            false
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Fingerprint,
                                        contentDescription = stringResource(R.string.erase),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }

                        val config = LocalConfiguration()

                        if (
                            config.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                            LocalWindowInfo().containerSize.height < 600
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(30.dp),
                                modifier = Modifier
                                    /* .background(
                                        MaterialTheme.colorScheme.surfaceContainer,
                                        RoundedCornerShape(32.dp)
                                    ) */
                                    .padding(20.dp)
                            ) {
                                content()
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    /* .background(
                                        MaterialTheme.colorScheme.surfaceContainer,
                                        RoundedCornerShape(32.dp)
                                    ) */
                                    .padding(20.dp)
                            ) {
                                content()
                            }
                        }
                    }
                }
            }
        }
    }

    fun requestAuth() {
        if (context is BaseActivity) {
            if (Settings.allowBiometric) {
                Log.d("Auth", "Requesting biometrical auth")
                blur.value = true
                context.requestAuthentication {
                    title = context.resources.getString(R.string.biometric)
                    negativeButtonText = context.resources.getString(R.string.use_password)
                    success {
                        authenticated = true
                        context.finishAfterTransition(R.anim.scale_down, R.anim.alpha_down)
                    }

                    always {
                        blur.value = false
                    }
                }
            }
        }
    }

    open fun setResult(resultCode: Int) {
        if (context is BaseActivity) {
            context.setResult(resultCode)
        }
    }

    open fun finish() {
        when {
            context is BaseActivity -> context.finish()
            this is ApplockerAuthOverlay -> hide()
        }
    }

    open fun finishAfterTransition(
        @AnimRes enterAnim: Int,
        @AnimRes exitAnim: Int
    ) {
        if (context is BaseActivity) {
            context.finishAfterTransition(enterAnim, exitAnim)
        } else {
            finish()
        }
    }
}