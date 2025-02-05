package ru.morozovit.ultimatesecurity.ui.crashreporter

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import ru.morozovit.android.async
import ru.morozovit.android.encodeJSON
import ru.morozovit.ultimatesecurity.App
import ru.morozovit.ultimatesecurity.App.Companion.githubRateLimitRemaining
import ru.morozovit.ultimatesecurity.BuildConfig
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.utils.EParser
import java.io.BufferedInputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

fun reportIssue(context: Context, exception: Throwable) {
    val handler = Handler(Looper.getMainLooper())
    async {
        with(context) {
            Log.d("IssueReporter", "Reporting an issue")
            val request = URL("https://api.github.com/repos/denis0001-dev/Toolbox-io/issues")
                .openConnection() as HttpsURLConnection
            request.requestMethod = "POST";
            request.setRequestProperty("Accept", "application/vnd.github.raw+json")
            request.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            request.setRequestProperty("Authorization", "Bearer ${App.GITHUB_TOKEN}")

            try {
                request.connect()
                request.outputStream.use {
                    val title = exception::class.simpleName?.encodeJSON()
                    val body = """
                        |_Этот отчет об ошибке был автоматически отправлен через Toolbox.io._
                        |
                        |### Конфигурация
                        |**Версия Android:** ${Build.VERSION.RELEASE}
                        |**Производитель:** ${Build.MANUFACTURER}
                        |**Бренд:** ${Build.BRAND}
                        |**Модель:** ${Build.MODEL}
                        |**Версия Toolbox.io**: ${BuildConfig.VERSION_NAME}
                        |
                        |### Ошибка
                        |```
                        |${"${EParser(exception)}".trim()}
                        |```
                    """
                        .trimMargin()
                        .encodeJSON()
                    val json = """
                        {
                            "title": "$title",
                            "body": "$body",
                            "assignees": ["denis0001-dev"],
                            "labels": ["приложение", "баг", "авто-отчет"]
                        }
                    """.trimIndent()

                    Log.d("IssueReporter", json)
                    it.write(json.toByteArray())
                }
                if (request.responseCode == 201) {
                    val input = BufferedInputStream(request.inputStream)
                    var c: Char;

                    val chars: MutableList<Char> = mutableListOf()

                    while (true) {
                        c = input.read().toChar()
                        if (c == 0.toChar() || c == '\uFFFF') break;
                        chars.add(c)
                    }
                    val response = String(chars.toCharArray())
                    val parsedResponse = JsonParser.parseString(response) as JsonObject
                    val number = parsedResponse["number"].asInt
                    handler.post {
                        Toast.makeText(
                            this,
                            resources.getString(R.string.issuecreated).format(number),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.d("IssueReporter", "Error. HTTP response code: ${request.responseCode}")
                    val errorInput = request.errorStream!!
                    var c: Char;

                    val chars: MutableList<Char> = mutableListOf()

                    while (true) {
                        c = errorInput.read().toChar()
                        if (c == 0.toChar() || c == '\uFFFF') break;
                        chars.add(c)
                    }
                    val response = String(chars.toCharArray())
                    Log.d("IssueReporter", "Error response: $response")
                    error("")
                }
            } catch (e: Exception) {
                Log.d("IssueReporter", "Error. \n${ru.morozovit.utils.EParser(e)}")
                handler.post {
                    Toast.makeText(
                        this,
                        resources.getString(R.string.smthwentwrong),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                runCatching {
                    githubRateLimitRemaining = request.getHeaderField("x-ratelimit-remaining").toLong()
                }
                request.disconnect()
            }
        }
    }
}