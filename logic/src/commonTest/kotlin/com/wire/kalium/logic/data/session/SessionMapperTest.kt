package com.wire.kalium.logic.data.session

import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.configuration.ServerConfigMapper
import com.wire.kalium.logic.data.id.IdMapper
import com.wire.kalium.logic.data.id.PersistenceQualifiedId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.network.api.QualifiedID
import com.wire.kalium.network.api.SessionDTO
import com.wire.kalium.network.tools.BackendConfig
import com.wire.kalium.persistence.dao.UserIDEntity
import com.wire.kalium.persistence.model.NetworkConfig
import com.wire.kalium.persistence.model.PersistenceSession
import io.mockative.Mock
import io.mockative.classOf
import io.mockative.given
import io.mockative.mock
import io.mockative.once
import io.mockative.verify
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import com.wire.kalium.network.api.UserId as UserIdDTO

class SessionMapperTest {

    @Mock
    val serverConfigMapper: ServerConfigMapper = mock(classOf<ServerConfigMapper>())

    @Mock
    val idMapper = mock(classOf<IdMapper>())

    private lateinit var sessionMapper: SessionMapper

    @BeforeTest
    fun setup() {
        sessionMapper = SessionMapperImpl(serverConfigMapper, idMapper)
    }


    @Test
    fun givenAnAuthSession_whenMappingToSessionCredentials_thenValuesAreMappedCorrectly() {
        val authSession: AuthSession = randomAuthSession()

        given(idMapper).invocation { toApiModel(authSession.userId) }
            .then { QualifiedID(authSession.userId.value, authSession.userId.domain) }
        val acuteValue: SessionDTO =
            with(authSession) { SessionDTO(UserIdDTO(userId.value, userId.domain), tokenType, accessToken, refreshToken) }

        val expectedValue: SessionDTO = sessionMapper.toSessionDTO(authSession)
        assertEquals(expectedValue, acuteValue)
    }

    @Test
    fun givenAnAuthSession_whenMappingToPersistenceSession_thenValuesAreMappedCorrectly() {
        val authSession: AuthSession = randomAuthSession()
        val networkConfig = with(authSession.serverConfig) {
            NetworkConfig(apiBaseUrl, accountsBaseUrl, webSocketBaseUrl, blackListUrl, teamsUrl, websiteUrl, title)
        }

        given(idMapper).invocation { toDaoModel(authSession.userId) }
            .then { PersistenceQualifiedId(authSession.userId.value, authSession.userId.domain) }
        given(serverConfigMapper).invocation { toNetworkConfig(authSession.serverConfig) }.then { networkConfig }

        val acuteValue: PersistenceSession =
            with(authSession) {
                PersistenceSession(
                    userId = UserIDEntity(userId.value, userId.domain),
                    tokenType = tokenType,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    networkConfig = networkConfig
                )
            }

        val expectedValue: PersistenceSession = sessionMapper.toPersistenceSession(authSession)
        assertEquals(expectedValue, acuteValue)
        verify(serverConfigMapper).invocation { toNetworkConfig(authSession.serverConfig) }.wasInvoked(exactly = once)
    }

    @Test
    fun givenAPersistenceSession_whenMappingFromPersistenceSession_thenValuesAreMappedCorrectly() {
        val persistenceSession: PersistenceSession = randomPersistenceSession()
        val serverConfig = with(persistenceSession.networkConfig) {
            ServerConfig(apiBaseUrl, accountBaseUrl, webSocketBaseUrl, blackListUrl, teamsUrl, websiteUrl, title)
        }

        given(idMapper).invocation { fromDaoModel(persistenceSession.userId) }
            .then { UserId(persistenceSession.userId.value, persistenceSession.userId.domain) }
        given(serverConfigMapper).invocation { fromNetworkConfig(persistenceSession.networkConfig) }.then { serverConfig }

        val acuteValue: AuthSession =
            with(persistenceSession) {
                AuthSession(
                    userId = UserId(userId.value, userId.domain),
                    tokenType = tokenType,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    serverConfig = serverConfig
                )
            }

        val expectedValue: AuthSession = sessionMapper.fromPersistenceSession(persistenceSession)
        assertEquals(expectedValue, acuteValue)
        verify(serverConfigMapper).invocation { fromNetworkConfig(persistenceSession.networkConfig) }.wasInvoked(exactly = once)
        verify(idMapper).invocation { fromDaoModel(persistenceSession.userId) }.wasInvoked(exactly = once)
    }


    private companion object {
        val randomString get() = Random.nextBytes(64).decodeToString()
        val userId = UserId("user_id", "user.domain.io")
        fun randomBackendConfig(): BackendConfig =
            BackendConfig(randomString, randomString, randomString, randomString, randomString, randomString, randomString)

        fun randomAuthSession(): AuthSession = AuthSession(userId, randomString, randomString, randomString, randomServerConfig())
        fun randomPersistenceSession(): PersistenceSession =
            PersistenceSession(UserIDEntity(userId.value, userId.domain), randomString, randomString, randomString, randomNetworkConfig())

        fun randomServerConfig(): ServerConfig =
            ServerConfig(randomString, randomString, randomString, randomString, randomString, randomString, randomString)

        fun randomNetworkConfig(): NetworkConfig =
            NetworkConfig(randomString, randomString, randomString, randomString, randomString, randomString, randomString)
    }
}
