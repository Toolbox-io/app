package io.toolbox.api

import android.os.Build
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.toolbox.BuildConfig
import kotlinx.serialization.Serializable
import ru.morozovit.android.utils.failOnError
import ru.morozovit.android.utils.jsonConfig

object IssuesAPI {
    private const val BASE_URL = "https://beta.toolbox-io.ru/api/issues/reportCrash"

    private val client by lazy {
        DefaultHTTPClient {
            jsonConfig {
                prettyPrint = true
            }
        }
    }

    @Serializable
    data class CrashReport(
        val androidVersion: String,
        val manufacturer: String,
        val brand: String,
        val model: String,
        val programVersion: String,
        val exceptionClass: String,
        val exceptionMsg: String,
        val exceptionStacktrace: String,
        val whatHappened: String
    )

    @Serializable
    data class CreatedIssue(
        val number: Int
    )

    suspend fun reportCrash(
        exception: String,
        message: String? = null
    ) =
        client
            .post(BASE_URL) {
                val title = exception.lines()[1].split(": ")

                setBody(
                    CrashReport(
                        androidVersion = Build.VERSION.RELEASE,
                        manufacturer = Build.MANUFACTURER,
                        brand = Build.BRAND,
                        model = Build.MODEL,
                        programVersion = BuildConfig.VERSION_NAME,
                        exceptionClass = title[0],
                        exceptionMsg = title[1].takeIf { it.isNotBlank() } ?: "<no message>",
                        exceptionStacktrace = exception.trim(),
                        whatHappened = message ?: ""
                    )
                )
            }
            .failOnError()
            .body<CreatedIssue>().number
}