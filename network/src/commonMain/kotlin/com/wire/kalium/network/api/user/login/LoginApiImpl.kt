package com.wire.kalium.network.api.user.login

import com.wire.kalium.network.utils.NetworkResponse
import com.wire.kalium.network.utils.wrapKaliumResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class LoginApiImpl(private val httpClient: HttpClient) : LoginApi {

    @Serializable
    internal data class LoginRequest(
        @SerialName("email") val email: String? = null,
        @SerialName("handle") val handle: String? = null,
        @SerialName("password") val password: String,
        @SerialName("label") val label: String
    )

    private fun LoginApi.LoginParam.toRequestBody(): LoginRequest {
        return when(this) {
            is LoginApi.LoginParam.LoginWithEmail -> LoginRequest(email = email, password = password, label = label)
            is LoginApi.LoginParam.LoginWithHandel -> LoginRequest(handle = handle, password = password, label = label)
        }
    }

    override suspend fun login(
        param: LoginApi.LoginParam,
        persist: Boolean
    ): NetworkResponse<LoginResponse> = wrapKaliumResponse {
        httpClient.post(PATH_LOGIN) {
            parameter(QUERY_PERSIST, persist)
            setBody(param.toRequestBody())
        }
    }

    private companion object {
        const val PATH_LOGIN = "/login"
        const val QUERY_PERSIST = "persist"
    }
}
