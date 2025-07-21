package io.toolbox.api

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

object Utils {
    private val client by lazy { DefaultHTTPClient() }

    suspend fun download(url: String) = runCatching {
        client
            .get(url)
            .bodyAsText()
    }.getOrNull()
}