@file:Suppress("NOTHING_TO_INLINE")

package io.toolbox.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ru.morozovit.android.defaultRequest
import ru.morozovit.android.jsonConfig
import ru.morozovit.android.logging

inline fun DefaultHTTPClient() = HttpClient(OkHttp) {
    jsonConfig {
        ignoreUnknownKeys = true
    }
    logging {
        logger = Logger.Companion.DEFAULT
        level = LogLevel.INFO
    }
    defaultRequest {
        contentType(ContentType.Application.Json)
    }
}