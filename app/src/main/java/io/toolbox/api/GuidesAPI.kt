package io.toolbox.api

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.toolbox.api.GuidesAPI.BASE_URL
import kotlinx.serialization.Serializable

/**
 * This class provides a Kotlin interface to the API endpoints of the Toolbox.io
 * website with the Ktor client.
 *
 * For JSON responses a **response model class** is returned, containing
 * all the properties from the JSON returned by the endpoint.
 *
 * The [BASE_URL] can be changed if the API moves to a different place.
 */
object GuidesAPI {
    private const val BASE_URL = "https://beta.toolbox-io.ru/guides"

    private val client by lazy {
        DefaultHTTPClient()
    }

    @Serializable
    data class GuideHeader(
        val DisplayName: String,
        val Icon: String
    )

    @Serializable
    data class Guide(
        val name: String,
        val header: GuideHeader
    )

    suspend fun list() =
        client
            .get("$BASE_URL/list")
            .body<Array<Guide>>()

    suspend fun list(name: String) =
        client
            .get("$BASE_URL/list/$name")
            .body<Guide>()
}