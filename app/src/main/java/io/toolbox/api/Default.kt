@file:Suppress("NOTHING_TO_INLINE")

package io.toolbox.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.ExperimentalSerializationApi
import ru.morozovit.android.defaultRequest
import ru.morozovit.android.jsonConfig
import ru.morozovit.android.logging

@OptIn(ExperimentalSerializationApi::class)
inline fun DefaultHTTPClient(
    crossinline config: HttpClientConfig<*>.() -> Unit = {}
) = HttpClient {
    jsonConfig {
        ignoreUnknownKeys = true
        allowComments = true
        allowTrailingComma = true
        isLenient = true
    }

    logging()

    defaultRequest {
        contentType(ContentType.Application.Json)
    }

    followRedirects = true

    config()
}