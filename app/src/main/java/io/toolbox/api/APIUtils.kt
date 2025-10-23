package io.toolbox.api

import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.toolbox.ui.main.PydanticError

suspend fun ResponseException.errorMessage(): String {
    runCatching {
        return response
            .body<Map<String, String>>()["detail"]!!
            .decodeErrorMessage()
    }

    runCatching {
        return response
            .body<PydanticError>()
            .error
            .decodeErrorMessage()
    }

    return response.status.toString()
}

fun String.decodeErrorMessage(): String =
    "^code (\\d+):(.*)$"
        .toRegex()
        .find(this)
        ?.let {
            when (it.groupValues[1].toIntOrNull() ?: return@let null) {
                // code 1 doesn't exist
                2 -> "Имя должно быть не меньше 3 символа"
                3 -> "Имя должно быть не больше 50 символов"
                4 -> "Имя может содержать только буквы, цифры, нижние подчеркивания и тире"
                5 -> "Email должен быть не больше 254 символов"
                6 -> "Имя не может быть пустым"
                // code 7 doesn't exist
                8 -> "Пароль не может быть пустым"
                9 -> "Пароль должен быть не меньше 8 символов"
                10 -> "Пароль должен быть не больше 128 символов"
                11 -> "Пароль слишком частый, попробуйте сделать его посложнее"
                12 -> "Пароль должен содержать буквы, цифвы и специальные символы"
                // end of codes

                else -> it.groupValues[2]
            }
        } ?: this