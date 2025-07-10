package io.toolbox

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioManager.STREAM_ALARM
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import io.toolbox.App.Companion.context
import io.toolbox.Settings.global_sharedPref
import io.toolbox.Settings.init
import io.toolbox.ui.protection.actions.intruderphoto.IntruderPhotoService.Companion.takePhoto
import ru.morozovit.android.checkHash
import ru.morozovit.android.hash
import ru.morozovit.android.isPlayingSafe
import ru.morozovit.android.ui.ThemeSetting
import java.io.IOException
import kotlin.concurrent.thread

@Suppress("MemberVisibilityCanBePrivate")
/**
 * An object containing all the settings of the app.
 *
 * The settings are split into categories:
 *
 * - **Keys** - where the encrypted passwords are stored
 * - **Main** - where general settings are stored
 * - **Actions** - where security actions settings are stored
 * - **Notifications** - where in-app notifications settings are stored
 *
 * _Self-explanatory settings are not included._
 *
 * ## Adding a settings class
 * 1. Add the following code to this object:
 *    ```kotlin
 *    object <CATEGORY_NAME> {
 *        private lateinit var <CATEGORY_NAME>_sharedPref: SharedPreferences
 *        private var init = false
 *
 *        fun init(context: Context) {
 *            if (!init) {
 *                <CATEGORY_NAME>_sharedPref = context.getSharedPreferences(
 *                    <CATEGORY_NAME>_LABEL,
 *                    Context.MODE_PRIVATE
 *                )
 *                init = true
 *            }
 *        }
 *    }
 *    ```
 *
 * 2. Add the following line to [init]'s **Init sub-objects** section:
 *
 *    ```
 *    <CATEGORY_NAME>.init(context)
 *    ```
 * 3. Add a `<CATEGORY_NAME>_LABEL` property to the **Settings sections** section of
 *    this object with a random string value.
 *
 *    ```
 *    val <CATEGORY_NAME>_LABEL = "qwredgscbbrkeag" // Replace the value with a unique random string
 *    ```
 *
 * Replace `CATEGORY_NAME` with the name of the new category in all steps.
 *
 * ## Adding a new property
 * Add the following code to the target object:
 *
 * - String property:
 *
 *   ```
 *   var <PROPERTY_NAME>
 *       get() = <CATEGORY_NAME>_sharedPref.getString(<PROPERTY_NAME>_LABEL, <DEFAULT_VALUE>)!!
 *       set(value) {
 *           <CATEGORY_NAME>_sharedPref.edit {
 *               putString(<PROPERTY_NAME>_LABEL, value)
 *           }
 *       }
 *   ```
 * - Boolean property:
 *
 *   ```
 *   var <PROPERTY_NAME>
 *       get() = <CATEGORY_NAME>_sharedPref.getBoolean(<PROPERTY_NAME>_LABEL, <DEFAULT_VALUE>)
 *       set(value) {
 *           <CATEGORY_NAME>_sharedPref.edit {
 *               putBoolean(<PROPERTY_NAME>_LABEL, value)
 *           }
 *       }
 *   ```
 *
 * Replace:
 * - `PROPERTY_NAME` with the property name.
 * - `CATEGORY_NAME` with the category object name (lowercase).
 * - `DEFAULT_VALUE` with the property default value if it doesn't exist.
 *
 * @see init
 * @see SharedPreferences
 */
object Settings {
    private lateinit var global_sharedPref: SharedPreferences
    private var init = false

    // Settings sections
    const val KEYS_LABEL = "gwegnagjh"
    const val MAIN_LABEL = "wshobjnwh"
    const val ACTIONS_LABEL = "rhnklahen"
    const val APPLOCKER_LABEL = "whnerhoerh"
    const val UNLOCK_PROTECTION_LABEL = "gewbnewrnh"
    const val TILES_LABEL = "ehnbgedkjhn"
    const val NOTIFICATIONS_LABEL = "ahbEFGJWH"
    const val DEVELOPER_LABEL = "erhgwiushwgwtvae"
    const val NOTIFICATION_HISTORY_LABEL = "wefnbiuahrwiefcnwe"
    const val ACCOUNT_LABEL = "wgjfhewaifjka3ghr4e"

    // Global settings
    const val ALLOW_BIOMETRIC_LABEL = "erjgeskh"
    const val APPLOCKER_ENCRYPTED_PASSWORD_LABEL = "hedrh"
    const val APP_ENCRYPTED_PASSWORD_LABEL = "waegnwg"
    const val DTMP_LABEL = "wfjnsrjfhsedjrg"

    // Sub-object settings
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
    const val REPLACE_PHOTOS_WITH_INTRUDER_LABEL = "ejewhtfsrgerith"
    const val REMOVE_DUPLICATES_LABEL = "errhgfwuw3eafvterw4"
    const val REMOVE_USELESS_NOTIFICATIONS_LABEL = "ergjeargizdNDUIearh"
    const val SHOW_MODE_LABEL = "weagnvbjdran"
    const val TOKEN_LABEL = "wfreabcsgbeiugfcewaer"
    const val USE_SENSORS_LABEL = "awfvdxrgbhrdfujbawert"
    const val TRIGGER_ON_CHARGER_LABEL = "wfvjsdergbjgtberst"

    /**
     * Main initialization function.
     *
     * It initializes the [global shared preferences][global_sharedPref] and
     * initializes the shared preferences of the sub-objects.
     *
     * @param context The context to use
     */
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
            Account.init(context)
            DTMP.init(context)

            init = true
        }
    }

    var update_dsa
        get() = global_sharedPref.getBoolean(UPDATE_DSA_LABEL, false)
        set(value) {
            global_sharedPref.edit {
                putBoolean(UPDATE_DSA_LABEL, value)
            }
        }

    var allowBiometric
        get() = global_sharedPref.getBoolean(ALLOW_BIOMETRIC_LABEL, false)
        set(value) {
            global_sharedPref.edit {
                putBoolean(ALLOW_BIOMETRIC_LABEL, value)
            }
        }

    var dontShowInRecents
        get() = global_sharedPref.getBoolean(DONT_SHOW_IN_RECENTS_LABEL, false)
        set(value) {
            global_sharedPref.edit {
                putBoolean(DONT_SHOW_IN_RECENTS_LABEL, value)
            }
        }

    var materialYouEnabled
        get() = global_sharedPref.getBoolean(MATERIAL_YOU_ENABLED_LABEL, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        set(value) {
            global_sharedPref.edit {
                putBoolean(MATERIAL_YOU_ENABLED_LABEL, value)
            }
        }

    var appTheme: ThemeSetting
        get() = ThemeSetting.entries[global_sharedPref.getInt(APP_THEME_LABEL, ThemeSetting.AsSystem.ordinal)]
        set(value) {
            global_sharedPref.edit {
                putInt(APP_THEME_LABEL, value.ordinal)
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
                    actions_sharedPref.edit {
                        putBoolean(ALARM_LABEL, value)
                    }
                }

            var current
                get() = actions_sharedPref.getString(CURRENT_CUSTOM_ALARM_LABEL, "")!!
                set(value) {
                    actions_sharedPref.edit {
                        putString(CURRENT_CUSTOM_ALARM_LABEL, value)
                    }
                }

            var customAlarms: Set<String>
                get() = actions_sharedPref.getStringSet(CUSTOM_ALARMS_LABEL, setOf())!!
                set(value) {
                    actions_sharedPref.edit {
                        putStringSet(CUSTOM_ALARMS_LABEL, value)
                    }
                }
        }
        object IntruderPhoto {
            var enabled
                get() = actions_sharedPref.getBoolean(INTRUDER_PHOTO_LABEL, false)
                set(value) {
                    actions_sharedPref.edit {
                        putBoolean(INTRUDER_PHOTO_LABEL, value)
                    }
                }

            var nopt
                get() = actions_sharedPref.getBoolean(INTRUDER_PHOTO_NOPT_LABEL, false)
                set(value) {
                    actions_sharedPref.edit {
                        putBoolean(INTRUDER_PHOTO_NOPT_LABEL, value)
                    }
                }
        }

        @Suppress("AssignedValueIsNeverRead")
        fun run(
            context: Context,
            mediaPlayer: MediaPlayer,
            audioManager: AudioManager,
            onCompletion: (() -> Unit)? = null
        ) {
            var intruderPhotoCompleted = false
            var alarmCompleted = false

            fun complete() {
                if (intruderPhotoCompleted && alarmCompleted) onCompletion?.invoke()
            }

            Log.d("Actions", "Security actions triggered!")
            // Take the required actions
            if (UnlockProtection.Alarm.enabled) {
                mediaPlayer.apply {
                    if (isPlaying) stop()
                    reset()
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )

                    val afd by lazy { context.assets.openFd("alarm.mp3") }

                    if (UnlockProtection.Alarm.current == "") {
                        setDataSource(afd)
                    } else {
                        try {
                            setDataSource(context, UnlockProtection.Alarm.current.toUri())
                        } catch (_: IOException) {
                            Log.w("DeviceAdmin", "Invalid custom alarm URI, falling back to default")
                            UnlockProtection.Alarm.current = ""
                            setDataSource(afd)
                        }
                    }
                    prepare()
                    start()

                    mediaPlayer.setOnCompletionListener {
                        alarmCompleted = true
                        complete()
                    }

                    thread {
                        while (mediaPlayer.isPlayingSafe) {
                            runCatching {
                                audioManager.setStreamVolume(
                                    STREAM_ALARM,
                                    audioManager.getStreamMaxVolume(STREAM_ALARM),
                                    0
                                )
                            }
                            Thread.sleep(100)
                        }
                    }
                }
            } else {
                alarmCompleted = true
            }
            if (UnlockProtection.IntruderPhoto.enabled) {
                takePhoto(context, "${System.currentTimeMillis()}") {
                    intruderPhotoCompleted = true
                    complete()
                }
            } else {
                intruderPhotoCompleted = true
            }

            complete()
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

        object Applocker: Key {
            private var passwordHash
                get() = keys_sharedPref.getString(APPLOCKER_ENCRYPTED_PASSWORD_LABEL, "")!!
                set(value) {
                    keys_sharedPref.edit {
                        putString(APPLOCKER_ENCRYPTED_PASSWORD_LABEL, value)
                    }
                }

            override fun set(password: String) {
                passwordHash = if (password.isBlank()) "" else password.hash()
            }

            override fun check(password: String): Boolean {
                if (passwordHash.isBlank() && password.isBlank()) {
                    return true
                }
                return password.checkHash(passwordHash)
            }

            override val isSet get() = passwordHash.isNotBlank()
        }

        object App: Key {
            private var passwordHash
                get() = keys_sharedPref.getString(APP_ENCRYPTED_PASSWORD_LABEL, "")!!
                set(value) {
                    keys_sharedPref.edit {
                        putString(APP_ENCRYPTED_PASSWORD_LABEL, value)
                    }
                }

            override fun set(password: String) {
                passwordHash = if (password.isBlank()) "" else password.hash()
            }

            override fun check(password: String): Boolean {
                if (passwordHash.isBlank() && password.isBlank()) {
                    return true
                }
                return password.checkHash(passwordHash)
            }

            override val isSet get() = passwordHash.isNotEmpty()
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
                applocker_sharedPref.edit {
                    putBoolean(ENABLED_LABEL, value)
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

        enum class ShowMode {
            FAKE_CRASH,
            PASSWORD_POPUP,
            FULLSCREEN_POPUP
        }

        var apps: Set<String>
            get() = applocker_sharedPref.getStringSet(SELECTED_APPS_LABEL, setOf())!!
            set(value) {
                applocker_sharedPref.edit {
                    putStringSet(SELECTED_APPS_LABEL, value)
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
                if (value in 0..UnlockMode.LONG_PRESS_OPEN_APP_AGAIN) applocker_sharedPref.edit {
                    putInt(UNLOCK_MODE_LABEL, value)
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

        fun getShowModeDescription(value: ShowMode, resources: Resources): String = when (value) {
            ShowMode.FAKE_CRASH -> resources.getString(R.string.fake_crash)
            ShowMode.FULLSCREEN_POPUP -> resources.getString(R.string.fullscreen_popup)
            ShowMode.PASSWORD_POPUP -> resources.getString(R.string.password_popup)
        }

        var used: Boolean
            get() = applocker_sharedPref.getBoolean(USED_LABEL, false)
            set(value) {
                applocker_sharedPref.edit {
                    putBoolean(USED_LABEL, value)
                }
            }

        var showMode: ShowMode
            get() = ShowMode.entries[applocker_sharedPref.getInt(SHOW_MODE_LABEL, 0)]
            set(value) {
                applocker_sharedPref.edit {
                    putInt(SHOW_MODE_LABEL, value.ordinal)
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
                unlockProtection_sharedPref.edit {
                    putBoolean(ENABLED_LABEL, value)
                }
            }

        var unlockAttempts
            get() = unlockProtection_sharedPref.getInt(UNLOCK_ATTEMPTS_LABEL, 2)
            set(value) = unlockProtection_sharedPref.edit {
                putInt(UNLOCK_ATTEMPTS_LABEL, value)
            }

        var fgServiceEnabled
            get() = unlockProtection_sharedPref.getBoolean(FG_SERVICE_ENABLED_LABEL, true)
            set(value) {
                unlockProtection_sharedPref.edit {
                    putBoolean(FG_SERVICE_ENABLED_LABEL, value)
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
                tiles_sharedPref.edit {
                    putBoolean(SLEEP_LABEL, value)
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
            notifications_sharedPref.edit {
                putBoolean(type, value)
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
                notificationHistory_sharedPref.edit {
                    putBoolean(ENABLED_LABEL, value)
                }
            }

        var removeDuplicates
            get() = notificationHistory_sharedPref.getBoolean(REMOVE_DUPLICATES_LABEL, true)
            set(value) {
                notificationHistory_sharedPref.edit {
                    putBoolean(REMOVE_DUPLICATES_LABEL, value)
                }
            }

        var removeUselessNotifications
            get() = notificationHistory_sharedPref.getBoolean(REMOVE_USELESS_NOTIFICATIONS_LABEL, true)
            set(value) {
                notificationHistory_sharedPref.edit {
                    putBoolean(REMOVE_USELESS_NOTIFICATIONS_LABEL, value)
                }
            }

        var apps: Set<String>
            get() = notificationHistory_sharedPref.getStringSet(
                SELECTED_APPS_LABEL,
                context
                    .packageManager
                    .getInstalledPackages(0)
                    .map { it.packageName }
                    .toSet()
            )!!
            set(value) {
                notificationHistory_sharedPref.edit {
                    putStringSet(SELECTED_APPS_LABEL, value)
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
                developer_sharedPref.edit {
                    putBoolean(REPLACE_PHOTOS_WITH_INTRUDER_LABEL, value)
                }
            }
    }

    object Account {
        private lateinit var account_sharedPref: SharedPreferences
        private var init = false

        fun init(context: Context) {
            if (!init) {
                account_sharedPref = context.getSharedPreferences(ACCOUNT_LABEL, Context.MODE_PRIVATE)
                init = true
            }
        }

        var token
            get() = account_sharedPref.getString(TOKEN_LABEL, "")!!
            set(value) {
                account_sharedPref.edit {
                    putString(TOKEN_LABEL, value)
                }
            }
    }

    object DTMP {
        private lateinit var dtmp_sharedPref: SharedPreferences
        private var init = false

        fun init(context: Context) {
            if (!init) {
                dtmp_sharedPref = context.getSharedPreferences(
                    DTMP_LABEL,
                    Context.MODE_PRIVATE
                )
                init = true
            }
        }

        var useSensors
            get() = dtmp_sharedPref.getBoolean(USE_SENSORS_LABEL, true)
            set(value) {
                dtmp_sharedPref.edit {
                    putBoolean(USE_SENSORS_LABEL, value)
                }
            }

        var triggerOnCharger
            get() = dtmp_sharedPref.getBoolean(TRIGGER_ON_CHARGER_LABEL, true)
            set(value) {
                dtmp_sharedPref.edit {
                    putBoolean(TRIGGER_ON_CHARGER_LABEL, value)
                }
            }
    }
}