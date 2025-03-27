package io.toolbox

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetFileDescriptor
import android.content.res.Resources
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioManager.STREAM_ALARM
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import io.toolbox.ui.Theme
import io.toolbox.ui.protection.actions.intruderphoto.IntruderPhotoService.Companion.takePhoto
import ru.morozovit.android.decrypt
import ru.morozovit.android.encrypt
import java.io.IOException
import kotlin.random.Random

@Suppress("MemberVisibilityCanBePrivate")
object Settings {
    private lateinit var global_sharedPref: SharedPreferences
    private var init = false

    const val KEYS_LABEL = "gwegnagjh"
    const val MAIN_LABEL = "wshobjnwh"
    const val ACTIONS_LABEL = "rhnklahen"
    const val APPLOCKER_LABEL = "whnerhoerh"
    const val UNLOCK_PROTECTION_LABEL = "gewbnewrnh"
    const val TILES_LABEL = "ehnbgedkjhn"
    const val NOTIFICATIONS_LABEL = "notificationTypes"
    const val DEVELOPER_LABEL = "developer"
    const val NOTIFICATION_HISTORY_LABEL = "notificationHistory"

    const val ALLOW_BIOMETRIC_LABEL = "erjgeskh"
    const val APPLOCKER_RANDOM_KEY_LABEL = "ejn"
    const val APPLOCKER_ENCRYPTED_PASSWORD_LABEL = "hedrh"
    const val APP_RANDOM_KEY_LABEL = "soeitge"
    const val APP_ENCRYPTED_PASSWORD_LABEL = "waegnwg"

    const val UPDATE_DSA_LABEL = "gsmwsojgnwg"
    const val DONT_SHOW_IN_RECENTS_LABEL = "grehbes"
    const val MATERIAL_YOU_ENABLED_LABEL = "vghwsjkrgn"
    const val APP_THEME_LABEL = "hgebngahnbe"
    const val ALARM_LABEL = "hbhnli"
    const val CURRENT_CUSTOM_ALARM_LABEL = "nghworhn"
    const val CUSTOM_ALARMS_LABEL = "qwaeftgn"
    const val INTRUDER_PHOTO_LABEL = "gwrsgbn"
    const val INTRUDER_PHOTO_NOPT_LABEL = "qglnqnegf"
    const val SELECTED_APPS_LABEL = "gnwlisohnrsb"
    const val UNLOCK_MODE_LABEL = "bnsrllhw"
    const val ENABLED_LABEL = "whgbnwrohn"
    const val UNLOCK_ATTEMPTS_LABEL = "gewrnwh"
    const val FG_SERVICE_ENABLED_LABEL = "hbjnwsokehgr"
    const val SLEEP_LABEL = "hbgewsrjkhn"
    const val USED_LABEL = "jtesnhjsertjsr"
    const val REPLACE_PHOTOS_WITH_INTRUDER_LABEL = "replacePhotosWithIntruder"

    fun init(context: Context) {
        if (!init) {
            global_sharedPref = context.getSharedPreferences(MAIN_LABEL, Context.MODE_PRIVATE)

            // Init sub-objects
            Keys.init(context)
            Applocker.init(context)
            UnlockProtection.init(context)
            Tiles.init(context)
            Notifications.init(context)
            NotificationHistory.init(context)
            Developer.init(context)
            init = true
        }
    }

    var update_dsa
        get() = global_sharedPref.getBoolean(UPDATE_DSA_LABEL, false)
        set(value) {
            with(global_sharedPref.edit()) {
                putBoolean(UPDATE_DSA_LABEL, value)
                apply()
            }
        }

    var allowBiometric
        get() = global_sharedPref.getBoolean(ALLOW_BIOMETRIC_LABEL, false)
        set(value) {
            with(global_sharedPref.edit()) {
                putBoolean(ALLOW_BIOMETRIC_LABEL, value)
                apply()
            }
        }

    var dontShowInRecents
        get() = global_sharedPref.getBoolean(DONT_SHOW_IN_RECENTS_LABEL, false)
        set(value) {
            with(global_sharedPref.edit()) {
                putBoolean(DONT_SHOW_IN_RECENTS_LABEL, value)
                apply()
            }
        }

    var materialYouEnabled
        get() = global_sharedPref.getBoolean(MATERIAL_YOU_ENABLED_LABEL, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        set(value) {
            with(global_sharedPref.edit()) {
                putBoolean(MATERIAL_YOU_ENABLED_LABEL, value)
                apply()
            }
        }

    var appTheme: Theme
        get() = Theme.entries[global_sharedPref.getInt(APP_THEME_LABEL, Theme.AsSystem.ordinal)]
        set(value) {
            with(global_sharedPref.edit()) {
                putInt(APP_THEME_LABEL, value.ordinal)
                apply()
            }
        }

    object Actions {
        private lateinit var actions_sharedPref: SharedPreferences
        private var init = false

        fun init(context: Context) {
            if (!init) {
                actions_sharedPref =
                    context.getSharedPreferences(ACTIONS_LABEL, Context.MODE_PRIVATE)
                init = true
            }
        }

        object Alarm {
            var enabled
                get() = actions_sharedPref.getBoolean(ALARM_LABEL, false)
                set(value) {
                    with(actions_sharedPref.edit()) {
                        putBoolean(ALARM_LABEL, value)
                        commit()
                    }
                }

            var current
                get() = actions_sharedPref.getString(CURRENT_CUSTOM_ALARM_LABEL, "")!!
                set(value) {
                    with(actions_sharedPref.edit()) {
                        putString(CURRENT_CUSTOM_ALARM_LABEL, value)
                        commit()
                    }
                }

            var customAlarms: Set<String>
                get() = actions_sharedPref.getStringSet(CUSTOM_ALARMS_LABEL, setOf())!!
                set(value) {
                    with(actions_sharedPref.edit()) {
                        putStringSet(CUSTOM_ALARMS_LABEL, value)
                        commit()
                    }
                }
        }
        object IntruderPhoto {
            var enabled
                get() = actions_sharedPref.getBoolean(INTRUDER_PHOTO_LABEL, false)
                set(value) {
                    with(actions_sharedPref.edit()) {
                        putBoolean(INTRUDER_PHOTO_LABEL, value)
                        commit()
                    }
                }

            var nopt
                get() = actions_sharedPref.getBoolean(INTRUDER_PHOTO_NOPT_LABEL, false)
                set(value) {
                    with(actions_sharedPref.edit()) {
                        putBoolean(INTRUDER_PHOTO_NOPT_LABEL, value)
                        commit()
                    }
                }
        }

        fun run(context: Context, mediaPlayer: MediaPlayer, audioManager: AudioManager) {
            // Take the required actions
            if (UnlockProtection.Alarm.enabled) {
                mediaPlayer.apply {
                    if (mediaPlayer.isPlaying) stop()
                    reset()
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                    if (UnlockProtection.Alarm.current == "") {
                        val afd: AssetFileDescriptor =
                            context.assets.openFd("alarm.mp3")
                        setDataSource(
                            afd.fileDescriptor,
                            afd.startOffset,
                            afd.length
                        )
                    } else {
                        try {
                            setDataSource(context, UnlockProtection.Alarm.current.toUri())
                        } catch (_: IOException) {
                            Log.w("DeviceAdmin", "Invalid custom alarm URI, falling back to default")
                            UnlockProtection.Alarm.current = ""
                            val afd: AssetFileDescriptor =
                                context.assets.openFd("alarm.mp3")
                            setDataSource(
                                afd.fileDescriptor,
                                afd.startOffset,
                                afd.length
                            )
                        }
                    }
                    prepare()
                    start()

                    Thread {
                        while (mediaPlayer.isPlaying) {
                            audioManager.setStreamVolume(STREAM_ALARM, audioManager.getStreamMaxVolume(STREAM_ALARM), 0)
                            Thread.sleep(100)
                        }
                    }.start()
                }
            }
            if (UnlockProtection.IntruderPhoto.enabled) {
                takePhoto(context, "${System.currentTimeMillis()}")
            }
        }
    }

    object Keys {
        private lateinit var keys_sharedPref: SharedPreferences
        private var init = false

        fun init(context: Context) {
            if (!init) {
                keys_sharedPref = context.getSharedPreferences(KEYS_LABEL, Context.MODE_PRIVATE)
                init = true
            }
        }

        interface Key {
            fun set(password: String)
            fun check(password: String): Boolean
            val isSet: Boolean
        }

        private fun generateKey() =
            Random.nextBytes(
                Random.nextInt(1, 16)
            )
                .map {
                    it.toInt().toChar()
                }
                .joinToString("")

        object Applocker: Key {
            private var randomKey: String
                get() {
                    var result = keys_sharedPref.getString(APPLOCKER_RANDOM_KEY_LABEL, null)
                    if (result == null) {
                        result = generateKey()
                        randomKey = result
                    }
                    return result
                }
                set(value) {
                    with(keys_sharedPref.edit()) {
                        putString(APPLOCKER_RANDOM_KEY_LABEL, value)
                        apply()
                    }
                }

            private var encryptedPassword
                get() = keys_sharedPref.getString(APPLOCKER_ENCRYPTED_PASSWORD_LABEL, "")!!
                set(value) {
                    with(keys_sharedPref.edit()) {
                        putString(APPLOCKER_ENCRYPTED_PASSWORD_LABEL, value)
                        apply()
                    }
                }

            override fun set(password: String) {
                randomKey = generateKey()
                encryptedPassword =
                    if (password.isEmpty()) ""
                    else randomKey.encrypt(password)
            }

            override fun check(password: String): Boolean {
                if (encryptedPassword.isEmpty() && password.isEmpty()) {
                    return true
                }
                val decryptedPassword = try {
                    encryptedPassword.decrypt(password)
                } catch (_: Exception) {
                    null
                }
                return decryptedPassword != null && decryptedPassword == randomKey
            }

            override val isSet get() = encryptedPassword.isNotEmpty()
        }

        object App: Key {
            private var randomKey: String
                get() {
                    var result = keys_sharedPref.getString(APP_RANDOM_KEY_LABEL, null)
                    if (result == null) {
                        result = generateKey()
                        randomKey = result
                    }
                    return result
                }
                set(value) {
                    with(keys_sharedPref.edit()) {
                        putString(APP_RANDOM_KEY_LABEL, value)
                        apply()
                    }
                }

            private var encryptedPassword
                get() = keys_sharedPref.getString(APP_ENCRYPTED_PASSWORD_LABEL, "")!!
                set(value) {
                    with(keys_sharedPref.edit()) {
                        putString(APP_ENCRYPTED_PASSWORD_LABEL, value)
                        apply()
                    }
                }

            override fun set(password: String) {
                if (password.isNotEmpty()) randomKey = generateKey()
                encryptedPassword =
                    if (password.isEmpty()) ""
                    else randomKey.encrypt(password)
            }

            override fun check(password: String): Boolean {
                if (encryptedPassword.isEmpty() && password.isEmpty()) {
                    return true
                }
                val decryptedPassword = try {
                    encryptedPassword.decrypt(password)
                } catch (_: Exception) {
                    null
                }
                return decryptedPassword != null && decryptedPassword == randomKey
            }

            override val isSet get() = encryptedPassword.isNotEmpty()
        }
    }

    object Applocker {
        private lateinit var applocker_sharedPref: SharedPreferences
        private var init = false

        fun init(context: Context) {
            if (!init) {
                applocker_sharedPref =
                    context.getSharedPreferences(APPLOCKER_LABEL, Context.MODE_PRIVATE)
                init = true
            }
        }

        var enabled
            get() = applocker_sharedPref.getBoolean(ENABLED_LABEL, false)
            set(value) {
                with(applocker_sharedPref.edit()) {
                    putBoolean(ENABLED_LABEL, value)
                    apply()
                }
            }

        object UnlockMode {
            const val NOTHING_SELECTED = -1
            const val LONG_PRESS_APP_INFO = 0
            const val LONG_PRESS_CLOSE = 1
            const val LONG_PRESS_TITLE = 2
            const val PRESS_TITLE = 3
            const val LONG_PRESS_OPEN_APP_AGAIN = 4
        }

        var apps: Set<String>
            get() = applocker_sharedPref.getStringSet(SELECTED_APPS_LABEL, setOf())!!
            set(value) {
                with(applocker_sharedPref.edit()) {
                    putStringSet(SELECTED_APPS_LABEL, value)
                    apply()
                }
            }

        private val DEFAULT_UNLOCK_MODE =
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
                UnlockMode.LONG_PRESS_APP_INFO
            else
                UnlockMode.LONG_PRESS_OPEN_APP_AGAIN

        var unlockMode: Int
            get() = applocker_sharedPref.getInt(UNLOCK_MODE_LABEL, DEFAULT_UNLOCK_MODE).let {
                return if (it == UnlockMode.NOTHING_SELECTED) DEFAULT_UNLOCK_MODE else it
            }
            set(value) {
                if (value in 0..UnlockMode.LONG_PRESS_OPEN_APP_AGAIN) with(applocker_sharedPref.edit()) {
                    putInt(UNLOCK_MODE_LABEL, value)
                    apply()
                } else {
                    throw IllegalArgumentException("The argument must be from 0 to PRESS_TITLE.")
                }
            }

        fun getUnlockModeDescription(value: Int, resources: Resources) = when (value) {
            UnlockMode.LONG_PRESS_APP_INFO -> resources.getString(R.string.lp_ai)
            UnlockMode.LONG_PRESS_CLOSE -> resources.getString(R.string.lp_c)
            UnlockMode.LONG_PRESS_TITLE -> resources.getString(R.string.lp_t)
            UnlockMode.PRESS_TITLE -> resources.getString(R.string.p_t)
            UnlockMode.LONG_PRESS_OPEN_APP_AGAIN -> resources.getString(R.string.lp_oaa)
            else -> ""
        }

        var used: Boolean
            get() = applocker_sharedPref.getBoolean(USED_LABEL, false)
            set(value) {
                with(applocker_sharedPref.edit()) {
                    putBoolean(USED_LABEL, value)
                    apply()
                }
            }
    }

    object UnlockProtection {
        private lateinit var unlockProtection_sharedPref: SharedPreferences
        private var init = false

        fun init(context: Context) {
            if (!init) {
                unlockProtection_sharedPref =
                    context.getSharedPreferences(UNLOCK_PROTECTION_LABEL, Context.MODE_PRIVATE)
                Actions.init(context)
                init = true
            }
        }

        var enabled
            get() = unlockProtection_sharedPref.getBoolean(ENABLED_LABEL, false)
            set(value) {
                with(unlockProtection_sharedPref.edit()) {
                    putBoolean(ENABLED_LABEL, value)
                    apply()
                }
            }

        var unlockAttempts
            get() = unlockProtection_sharedPref.getInt(UNLOCK_ATTEMPTS_LABEL, 2)
            set(value) = with(unlockProtection_sharedPref.edit()) {
                putInt(UNLOCK_ATTEMPTS_LABEL, value)
                apply()
            }

        var fgServiceEnabled
            get() = unlockProtection_sharedPref.getBoolean(FG_SERVICE_ENABLED_LABEL, true)
            set(value) {
                with(unlockProtection_sharedPref.edit()) {
                    putBoolean(FG_SERVICE_ENABLED_LABEL, value)
                    apply()
                }
            }

        val Alarm = Actions.Alarm
        val IntruderPhoto = Actions.IntruderPhoto
    }

    object Tiles {
        private lateinit var tiles_sharedPref: SharedPreferences
        private var init = false

        fun init(context: Context) {
            if (!init) {
                tiles_sharedPref = context.getSharedPreferences(TILES_LABEL, Context.MODE_PRIVATE)
                init = true
            }
        }

        var sleep
            get() = tiles_sharedPref.getBoolean(SLEEP_LABEL, false)
            set(value) {
                with(tiles_sharedPref.edit()) {
                    putBoolean(SLEEP_LABEL, value)
                    apply()
                }
            }
    }

    object Notifications {
        private lateinit var notifications_sharedPref: SharedPreferences
        private var init = false

        fun init(context: Context) {
            if (!init) {
                notifications_sharedPref = context.getSharedPreferences(NOTIFICATIONS_LABEL, Context.MODE_PRIVATE)
                init = true
            }
        }

        operator fun get(type: String): Boolean {
            return notifications_sharedPref.getBoolean(type, true)
        }

        operator fun set(type: String, value: Boolean) {
            with(notifications_sharedPref.edit()) {
                putBoolean(type, value)
                apply()
            }
        }
    }

    object NotificationHistory {
        private lateinit var notificationHistory_sharedPref: SharedPreferences
        private var init = false

        fun init(context: Context) {
            if (!init) {
                notificationHistory_sharedPref = context.getSharedPreferences(
                    NOTIFICATION_HISTORY_LABEL,
                    Context.MODE_PRIVATE
                )
                init = true
            }
        }

        var enabled
            get() = notificationHistory_sharedPref.getBoolean(ENABLED_LABEL, false)
            set(value) {
                with(notificationHistory_sharedPref.edit()) {
                    putBoolean(ENABLED_LABEL, value)
                    apply()
                }
            }
    }

    object Developer {
        private lateinit var developer_sharedPref: SharedPreferences
        private var init = false

        fun init(context: Context) {
            if (!init) {
                developer_sharedPref = context.getSharedPreferences(DEVELOPER_LABEL, Context.MODE_PRIVATE)
                init = true
            }
        }

        var replacePhotosWithIntruder
            get() = developer_sharedPref.getBoolean(REPLACE_PHOTOS_WITH_INTRUDER_LABEL, false)
            set(value) {
                with(developer_sharedPref.edit()) {
                    putBoolean(REPLACE_PHOTOS_WITH_INTRUDER_LABEL, value)
                    apply()
                }
            }
    }
}