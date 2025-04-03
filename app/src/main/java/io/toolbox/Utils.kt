package io.toolbox

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.util.Log
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import io.toolbox.App.Companion.GITHUB_API_VERSION
import io.toolbox.App.Companion.GITHUB_TOKEN
import io.toolbox.App.Companion.githubRateLimitRemaining
import io.toolbox.ui.MainActivity
import ru.morozovit.utils.EParser
import java.io.BufferedInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

fun download(url: String): String? {
    var retryWithHttps = false
    while (true) {
        var request: HttpURLConnection? = null
        try {
            request = URL(
                if (retryWithHttps)
                    url.replace("http://", "https://")
                else url
            ).openConnection() as HttpURLConnection
            request.requestMethod = "GET"
            request.connect()
            if (request.responseCode == 200) {
                val input = BufferedInputStream(request.inputStream)
                var c: Char

                val chars: MutableList<Char> = mutableListOf()

                while (true) {
                    c = input.read().toChar()
                    if (c == 0.toChar() || c == '\uFFFF') break
                    chars.add(c)
                }
                return String(chars.toCharArray())
            } else {
                Log.d("IssueReporter", "Error. HTTP response code: ${request.responseCode}")
                val errorInput = request.errorStream!!
                var c: Char

                val chars: MutableList<Char> = mutableListOf()

                while (true) {
                    c = errorInput.read().toChar()
                    if (c == 0.toChar() || c == '\uFFFF') break
                    chars.add(c)
                }
                val response = String(chars.toCharArray())
                Log.d("Download", "Error response: $response")
                error("")
            }
        } catch (e: Exception) {
            Log.d("Download", "Error. \n${EParser(e)}")
            if (e is IOException && e.message?.startsWith("Cleartext HTTP traffic to ") == true) {
                retryWithHttps = true
                continue
            }
            return null
        } finally {
            request?.disconnect()
        }
    }
}

fun getContents(
    dir: String,
    owner: String = "Toolbox-io",
    repo: String = "Toolbox-io",
    isFullUrl: Boolean = false
): JsonElement? {
    val request = URL(
        if (isFullUrl) {
            dir
        } else {
            "https://api.github.com/repos/$owner/$repo/contents/$dir"
        }
    ).openConnection() as HttpURLConnection
    request.requestMethod = "GET"
    request.setRequestProperty("Accept", "application/vnd.github+json")
    request.setRequestProperty("X-GitHub-Api-Version", GITHUB_API_VERSION)
    request.setRequestProperty("Authorization", "Bearer $GITHUB_TOKEN")

    try {
        request.connect()
        if (request.responseCode == 200) {
            val input = BufferedInputStream(request.inputStream)
            var c: Char

            val chars: MutableList<Char> = mutableListOf()

            while (true) {
                c = input.read().toChar()
                if (c == 0.toChar() || c == '\uFFFF') break
                chars.add(c)
            }
            val response = String(chars.toCharArray())
            return JsonParser.parseString(response)
        } else {
            Log.d("Stories", "Error. HTTP response code: ${request.responseCode}")
            val errorInput = request.errorStream!!
            var c: Char

            val chars: MutableList<Char> = mutableListOf()

            while (true) {
                c = errorInput.read().toChar()
                if (c == 0.toChar() || c == '\uFFFF') break
                chars.add(c)
            }
            val response = String(chars.toCharArray())
            Log.d("Stories", "Error response: $response")
            error("")
        }
    } catch (e: Exception) {
        Log.d("Stories", "Error. \n${EParser(e)}")
        return null
    } finally {
        runCatching {
            githubRateLimitRemaining = request.getHeaderField("x-ratelimit-remaining").toLong()
        }
        request.disconnect()
    }
}

fun Context.mainActivity() {
    startActivity(
        Intent(this, MainActivity::class.java).apply {
            flags = FLAG_ACTIVITY_NEW_TASK
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
    )
}