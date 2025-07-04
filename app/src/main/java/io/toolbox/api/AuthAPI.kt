package io.toolbox.api

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.toolbox.Settings
import io.toolbox.api.AuthAPI.BASE_URL
import kotlinx.serialization.Serializable

/**
 * This class provides a Kotlin interface to the account API endpoints of the Toolbox.io
 * website with the Ktor client.
 *
 * For JSON responses a **response model class** is returned, containing
 * all the properties from the JSON returned by the endpoint.
 *
 * The [BASE_URL] can be changed if the API moves to a different place.
 */
object AuthAPI {
    private const val BASE_URL = "https://beta.toolbox-io.ru/api/auth"

    @Serializable
    private data class UserCreate(
        val username: String,
        val email: String,
        val password: String
    )
    @Serializable
    private data class UserLogin(
        val username: String,
        val password: String
    )
    @Serializable
    private data class UserResetPassword(
        val token: String,
        val new_password: String
    )
    @Serializable
    private data class UserCheckAuth(
        val authenticated: Boolean,
        val user: String
    )
    @Serializable
    private data class UserVerifyEmail(val email: String)

    @Serializable
    data class UserInfo(
        val id: Int,
        val username: String,
        val email: String,
        val created_at: String
    )

    private val client by lazy {
        DefaultHTTPClient()
    }

    suspend fun register(
        username: String,
        email: String,
        password: String
    ): UserInfo =
        client.post("$BASE_URL/register") {
            setBody(UserCreate(username, email, password))
        }.body()

    suspend fun login(username: String, password: String) =
        client
            .post("$BASE_URL/login") {
                setBody(UserLogin(username, password))
            }
            .body<Map<String, String>>()["access_token"]!!

    suspend fun logout() =
        client.post("$BASE_URL/logout") {
            header("Authorization", "Bearer ${Settings.Account.token}")
        }

    suspend fun checkAuth(): Boolean {
        val result = client
            .get("$BASE_URL/check-auth") {
                header("Authorization", "Bearer ${Settings.Account.token}")
            }
            .takeIf { it.status.value == 200 }
            ?.body<UserCheckAuth>()
            ?.authenticated == true

        if (!result) Settings.Account.token = "" // Delete bad token

        return result
    }

    suspend fun userInfo(): UserInfo =
        client.get("$BASE_URL/user-info") {
            header("Authorization", "Bearer ${Settings.Account.token}")
        }.body()

    suspend fun requestReset(email: String) =
        client.post("$BASE_URL/request-reset") {
            setBody(mapOf("email" to email))
        }

    suspend fun resetPassword(token: String, newPassword: String) =
        client.post("$BASE_URL/reset-password") {
            setBody(UserResetPassword(token, newPassword))
        }

    suspend fun verifyEmail(email: String) =
        client.post("$BASE_URL/verify-email") {
            setBody(UserVerifyEmail(email))
        }

    suspend fun isLoggedIn() = Settings.Account.token.isNotBlank() && checkAuth()
}