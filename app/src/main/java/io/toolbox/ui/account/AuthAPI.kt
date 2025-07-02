package io.toolbox.ui.account

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.toolbox.Settings.Account.token
import kotlinx.serialization.Serializable
import ru.morozovit.android.jsonConfig
import ru.morozovit.android.logging

object AuthAPI {
    private const val BASE_URL = "https://beta.toolbox-io.ru"

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
    data class UserInfo(
        val id: Int,
        val username: String,
        val email: String,
        val created_at: String
    )

    private val client by lazy {
        HttpClient(OkHttp) {
            jsonConfig {
                ignoreUnknownKeys = true
            }
            logging()
        }
    }

    suspend fun register(
        username: String,
        email: String,
        password: String
    ): UserInfo =
        client.post("$BASE_URL/api/auth/register") {
            setBody(UserCreate(username, email, password))
        }.body()

    suspend fun login(username: String, password: String) =
        client
            .post("$BASE_URL/api/auth/login") {
                setBody(UserLogin(username, password))
            }
            .body<Map<String, String>>()["token"]!!

    suspend fun logout() =
        client.post("$BASE_URL/api/auth/logout") {
            header("Authorization", "Bearer $token")
        }

    suspend fun checkAuth(): Boolean =
        client
            .get("$BASE_URL/api/auth/check-auth") {
                header("Authorization", "Bearer $token")
            }
            .body<Map<String, Any>>()
            .getOrDefault("authenticated", false) as Boolean

    suspend fun userInfo(): UserInfo =
        client.get("$BASE_URL/api/auth/user-info") {
            header("Authorization", "Bearer $token")
        }.body()

    suspend fun requestReset(email: String) =
        client.post("$BASE_URL/api/auth/request-reset") {
            setBody(mapOf("email" to email))
        }

    suspend fun resetPassword(token: String, newPassword: String) =
        client.post("$BASE_URL/api/auth/reset-password") {
            setBody(UserResetPassword(token, newPassword))
        }

    suspend fun verifyEmail(email: String) =
        client.post("$BASE_URL/api/auth/verify-email") {
            setBody(mapOf("email" to email))
        }
}