package ru.morozovit.ultimatesecurity

import android.util.Log
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import ru.morozovit.ultimatesecurity.App.Companion.GITHUB_API_VERSION
import ru.morozovit.ultimatesecurity.App.Companion.githubRateLimitRemaining
import java.io.BufferedInputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

fun download(url: String): String? {
    val request = URL(url).openConnection() as HttpsURLConnection
    request.requestMethod = "GET"

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
        Log.d("Download", "Error. \n${ru.morozovit.utils.EParser(e)}")
        return null
    } finally {
        request.disconnect()
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
    ).openConnection() as HttpsURLConnection
    request.requestMethod = "GET"
    request.setRequestProperty("Accept", "application/vnd.github+json")
    request.setRequestProperty("X-GitHub-Api-Version", GITHUB_API_VERSION)
    request.setRequestProperty("Authorization", "Bearer ${App.GITHUB_TOKEN}")

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
        Log.d("Stories", "Error. \n${ru.morozovit.utils.EParser(e)}")
        return null
    } finally {
        runCatching {
            githubRateLimitRemaining = request.getHeaderField("x-ratelimit-remaining").toLong()
        }
        request.disconnect()
    }
}