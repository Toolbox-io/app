package ru.morozovit.ultimatesecurity.share

import android.content.Intent
import android.content.Intent.EXTRA_TEXT
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import ru.morozovit.android.BetterActivityResult.registerActivityForResult
import ru.morozovit.android.getFileName
import ru.morozovit.android.getParcelableExtraAs

class SaveActivity : AppCompatActivity() {
    companion object {
        const val TYPE_TEXT = 0
        const val TYPE_URI = 1
        const val TYPE_NONE = -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityLauncher = registerActivityForResult(this)

        val dataType = run t@ {
            runCatching {
                val text = intent.getStringExtra(EXTRA_TEXT)
                if (text != null) {
                    return@t TYPE_TEXT
                }
                intent.getParcelableExtraAs<Uri>(Intent.EXTRA_STREAM)
                return@t TYPE_URI
            }
            return@t TYPE_NONE
        }

        assert(dataType in 0..1)

        if (intent.action == Intent.ACTION_SEND) {
            activityLauncher.launch(
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    when (dataType) {
                        TYPE_TEXT -> putExtra(
                            Intent.EXTRA_TITLE,
                            intent
                                .getStringExtra(EXTRA_TEXT)!!
                                .split("\n")
                                .first()
                        )

                        TYPE_URI -> putExtra(
                            Intent.EXTRA_TITLE,
                            getFileName(
                                intent.getParcelableExtraAs<Uri>(Intent.EXTRA_STREAM)
                            )
                        )
                    }
                    type = intent.type
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
            ) {
                try {
                    if (it.resultCode == RESULT_OK) {
                        val uri = it.data!!.data!!
                        contentResolver.openOutputStream(uri)!!.use { s ->
                            when (dataType) {
                                TYPE_TEXT -> {
                                    s.write(
                                        intent
                                            .getCharSequenceExtra(EXTRA_TEXT)
                                            .toString()
                                            .toByteArray()
                                    )
                                }
                                TYPE_URI -> {
                                    contentResolver.openInputStream(
                                        intent.getParcelableExtraAs<Uri>(Intent.EXTRA_STREAM)
                                    )!!.use { inputStream ->
                                        inputStream.copyTo(s)
                                    }
                                }
                                else -> throw IllegalStateException("Unsupported data type")
                            }
                        }
                    } else {
                        throw IllegalStateException("result is not ok")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                finish()
            }
        } else {
            Log.e("Save", "action is not send")
            finish()
        }
    }
}