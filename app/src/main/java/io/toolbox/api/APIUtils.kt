package io.toolbox.api

import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.toolbox.ui.main.PydanticError

suspend fun ResponseException.errorMessage(): String {
    runCatching {
        return response
            .body<Map<String, String>>()["detail"]!!
    }
    runCatching {
        return response
            .body<PydanticError>()
            .error
    }

    return "Unknown error"
}