@file:Suppress("NOTHING_TO_INLINE")

package io.toolbox.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.ExperimentalSerializationApi
import ru.morozovit.android.defaultRequest
import ru.morozovit.android.jsonConfig
import ru.morozovit.android.logging

@OptIn(ExperimentalSerializationApi::class)
fun DefaultHTTPClient() = HttpClient {
    jsonConfig {
        ignoreUnknownKeys = true
        allowComments = true
        allowTrailingComma = true
        isLenient = true
    }

    logging {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
    }

    defaultRequest {
        contentType(ContentType.Application.Json)
    }

    followRedirects = true
}