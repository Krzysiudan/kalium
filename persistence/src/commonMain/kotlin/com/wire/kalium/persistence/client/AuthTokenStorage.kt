/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.kalium.persistence.client

import com.wire.kalium.persistence.dao.UserIDEntity
import com.wire.kalium.persistence.kmmSettings.KaliumPreferences
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthTokenEntity(
    @SerialName("user_id") val userId: UserIDEntity,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("cookie_label") val cookieLabel: String?
)

@Serializable
data class ProxyCredentialsEntity(
    @SerialName("username") val username: String,
    @SerialName("password") val password: String,
)

class AuthTokenStorage internal constructor(
    private val kaliumPreferences: KaliumPreferences
) {
    fun addOrReplace(authTokenEntity: AuthTokenEntity, proxyCredentialsEntity: ProxyCredentialsEntity?) {
        kaliumPreferences.putSerializable(
            tokenKey(authTokenEntity.userId),
            authTokenEntity,
            AuthTokenEntity.serializer()
        )

        proxyCredentialsEntity?.let {
            kaliumPreferences.putSerializable(proxyCredentialsKey(authTokenEntity.userId), it, ProxyCredentialsEntity.serializer())
        }
    }

    fun updateToken(
        userId: UserIDEntity,
        accessToken: String,
        tokenType: String,
        refreshToken: String?,
    ): AuthTokenEntity {
        val key = tokenKey(userId)
        val newToken = kaliumPreferences.getSerializable(key, AuthTokenEntity.serializer())
            ?.let {
                it.copy(accessToken = accessToken, refreshToken = refreshToken ?: it.refreshToken, tokenType = tokenType)
            } ?: error("No token found for user")

        kaliumPreferences.putSerializable(
            key,
            newToken,
            AuthTokenEntity.serializer()
        )
        return newToken
    }

    // TODO: make suspendable
    fun getToken(userId: UserIDEntity): AuthTokenEntity? =
        kaliumPreferences.getSerializable(
            tokenKey(userId),
            AuthTokenEntity.serializer()
        )

    fun deleteToken(userId: UserIDEntity) {
        kaliumPreferences.remove(tokenKey(userId))
        kaliumPreferences.remove(proxyCredentialsKey(userId))
    }

    fun proxyCredentials(userId: UserIDEntity): ProxyCredentialsEntity? =
        kaliumPreferences.getSerializable(proxyCredentialsKey(userId), ProxyCredentialsEntity.serializer())

    private fun tokenKey(userId: UserIDEntity): String {
        return "user_tokens_${userId.value}@${userId.domain}"
    }

    private fun proxyCredentialsKey(userId: UserIDEntity): String {
        return "proxy_credentials_${userId.value}@${userId.domain}"
    }
}
