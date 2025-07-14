package io.toolbox.share

import android.content.Intent
import android.content.Intent.EXTRA_TEXT
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ru.morozovit.android.utils.activityResultLauncher
import ru.morozovit.android.utils.getFileName
import ru.morozovit.android.utils.getParcelableExtraAs
import ru.morozovit.android.utils.runOrLog

class SaveActivity: AppCompatActivity() {
    companion object {
        const val TYPE_TEXT = 0
        const val TYPE_URI = 1
        const val TYPE_NONE = -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityLauncher = activityResultLauncher

        val dataType = run type@ {
            runCatching {
                if (intent.getStringExtra(EXTRA_TEXT) != null) {
                    return@type TYPE_TEXT
                }
                if (intent.getParcelableExtraAs<Uri>(Intent.EXTRA_STREAM) != null) {
                    return@type TYPE_URI
                }
            }
            TYPE_NONE
        }

        if (dataType !in 0..1) {
            Toast.makeText(
                this,
                "Invalid data type",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

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
                                intent.getParcelableExtraAs<Uri>(Intent.EXTRA_STREAM)!!
                            )
                        )
                    }
                    type = intent.type
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
            ) {
                runOrLog("SaveActivity") {
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
                                        intent.getParcelableExtraAs<Uri>(Intent.EXTRA_STREAM)!!
                                    )!!.use { inputStream ->
                                        inputStream.copyTo(s)
                                    }
                                }
                            }
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Failed to save file",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                finish()
            }
        } else {
            Log.e("Save", "action is not send")
            finish()
        }
    }
}