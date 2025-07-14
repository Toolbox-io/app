package io.toolbox.api

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.toolbox.Settings.Account.token
import io.toolbox.api.AuthAPI.BASE_URL
import kotlinx.serialization.Serializable
import ru.morozovit.android.utils.failOnError

/**
 * This class provides a Kotlin interface to the account API endpoints of
 * the Toolbox.io website with the Ktor client.
 *
 * For JSON responses a **response model class** is returned, containing
 * all the properties from the JSON returned by the endpoint.
 *
 * The [BASE_URL] can be changed if the API moves to a different place.
 */
object AuthAPI {
    private const val BASE_URL = "https://beta.toolbox-io.ru/api/auth"

    /////////////////////
    // Internal models //
    /////////////////////

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
    private data class UserPasswordChange(
        val current_password: String,
        val new_password: String
    )

    /////////////////////
    // Returned models //
    /////////////////////

    @Serializable
    data class UserInfo(
        val id: Int,
        val username: String,
        val email: String,
        val created_at: String
    )

    ///////////////////
    // API endpoints //
    ///////////////////

    private val client by lazy { DefaultHTTPClient() }

    suspend fun register(
        username: String,
        email: String,
        password: String
    ): UserInfo =
        client
            .post("$BASE_URL/register") {
                setBody(UserCreate(username, email, password))
            }
            .failOnError()
            .body()

    suspend fun login(username: String, password: String): String {
        val result = client
            .post("$BASE_URL/login") {
                setBody(UserLogin(username, password))
            }
            .failOnError()
            .body<Map<String, String>>()["access_token"]!!
        token = result
        return result
    }

    suspend fun logout() {
        client.post("$BASE_URL/logout") {
            header("Authorization", "Bearer $token")
        }
        token = ""
    }

    suspend fun checkAuth(): Boolean {
        val result = client
            .get("$BASE_URL/check-auth") {
                header("Authorization", "Bearer $token")
            }
            .takeIf { it.status.value == 200 }
            ?.body<UserCheckAuth>()
            ?.authenticated == true

        if (!result) token = "" // Delete bad token

        return result
    }

    suspend fun userInfo(): UserInfo =
        client
            .get("$BASE_URL/user-info") {
                header("Authorization", "Bearer $token")
            }
            .failOnError()
            .body()

    // TODO reset password screen
    suspend fun requestReset(email: String) {
        client
            .post("$BASE_URL/request-reset") {
                setBody(mapOf("email" to email))
            }
            .failOnError()
    }

    suspend fun resetPassword(token: String, newPassword: String) {
        client
            .post("$BASE_URL/reset-password") {
                setBody(UserResetPassword(token, newPassword))
            }
            .failOnError()
    }

    suspend fun sendVerifyEmail(email: String) {
        client
            .post("$BASE_URL/verify-email") {
                setBody(UserVerifyEmail(email))
            }
            .failOnError()
    }

    suspend fun changePassword(
        oldPassword: String,
        newPassword: String
    ) = client
        .post("$BASE_URL/change-password") {
            header("Authorization", "Bearer $token")
            setBody(UserPasswordChange(oldPassword, newPassword))
        }
        .failOnError()

    suspend fun isLoggedIn() = token.isNotBlank() && checkAuth()

    suspend fun verifyEmail(code: Int) {
        client
            .post("$BASE_URL/verify?code=$code")
            .failOnError()
    }
}