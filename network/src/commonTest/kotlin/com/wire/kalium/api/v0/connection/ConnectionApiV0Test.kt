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

package com.wire.kalium.api.v0.connection

import com.wire.kalium.api.ApiTest
import com.wire.kalium.model.connection.ConnectionRequestsJson
import com.wire.kalium.model.connection.ConnectionResponsesJson
import com.wire.kalium.network.api.base.authenticated.connection.ConnectionApi
import com.wire.kalium.network.api.base.authenticated.connection.ConnectionStateDTO
import com.wire.kalium.network.api.base.model.UserId
import com.wire.kalium.network.api.v0.authenticated.ConnectionApiV0
import com.wire.kalium.network.utils.isSuccessful
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

internal class ConnectionApiV0Test : ApiTest() {

    @Test
    fun givenAGetConnectionsRequest_whenRequestingAllConnectionsWithSuccess_thenRequestShouldBeConfiguredCorrectly() = runTest {
        val networkClient = mockAuthenticatedNetworkClient(
            GET_CONNECTIONS_RESPONSE.rawJson,
            statusCode = HttpStatusCode.OK,
            assertion = {
                assertJson()
                assertPost()
                assertJsonBodyContent(GET_CONNECTIONS_NO_PAGING_REQUEST.rawJson)
                assertPathEqual(PATH_CONNECTIONS)
            }
        )

        val connectionApi: ConnectionApi = ConnectionApiV0(networkClient)
        connectionApi.fetchSelfUserConnections(pagingState = null)
    }

    @Test
    fun givenAGetConnectionsRequestWithPaging_whenRequestingAllConnectionsWithSuccess_thenRequestShouldBeConfiguredCorrectly() = runTest {
        val networkClient = mockAuthenticatedNetworkClient(
            GET_CONNECTIONS_RESPONSE.rawJson,
            statusCode = HttpStatusCode.OK,
            assertion = {
                assertJson()
                assertPost()
                assertJsonBodyContent(GET_CONNECTIONS_WITH_PAGING_REQUEST.rawJson)
                assertPathEqual(PATH_CONNECTIONS)
            }
        )

        val connectionApi: ConnectionApi = ConnectionApiV0(networkClient)
        connectionApi.fetchSelfUserConnections(pagingState = GET_CONNECTIONS_WITH_PAGING_REQUEST.serializableData)
    }

    @Test
    fun givenACreationRequest_whenRequestingAConnectionWithAnUser_thenShouldReturnsACorrectConnectionResponse() = runTest {
        // given
        val userId = UserId("user_id", "domain_id")
        val httpClient = mockAuthenticatedNetworkClient(
            CREATE_CONNECTION_RESPONSE.rawJson,
            statusCode = HttpStatusCode.OK,
            assertion = {
                assertJson()
                assertPost()
                assertPathEqual("$PATH_CONNECTIONS_ENDPOINT/${userId.domain}/${userId.value}")
            }
        )
        val connectionApi = ConnectionApiV0(httpClient)

        // when
        val response = connectionApi.createConnection(userId)

        // then
        assertTrue(response.isSuccessful())

    }

    @Test
    fun givenAConnectionRequestUpdate_whenInvokingWithAnUserAndAConnectionStatus_thenShouldReturnsACorrectConnectionResponse() =
        runTest {
            // given
            val userId = UserId("user_id", "domain_id")
            val httpClient = mockAuthenticatedNetworkClient(
                CREATE_CONNECTION_RESPONSE.rawJson,
                statusCode = HttpStatusCode.OK,
                assertion = {
                    assertJson()
                    assertPut()
                    assertPathEqual("$PATH_CONNECTIONS_ENDPOINT/${userId.domain}/${userId.value}")
                    assertJsonBodyContent(GET_CONNECTION_STATUS_REQUEST.rawJson)
                }
            )
            val connectionApi = ConnectionApiV0(httpClient)

            // when
            val response = connectionApi.updateConnection(userId, ConnectionStateDTO.ACCEPTED)

            // then
            assertTrue(response.isSuccessful())
        }

    private companion object {
        const val PATH_CONNECTIONS = "/list-connections"
        const val PATH_CONNECTIONS_ENDPOINT = "/connections"

        val GET_CONNECTIONS_RESPONSE = ConnectionResponsesJson.GetConnections.validGetConnections
        val CREATE_CONNECTION_RESPONSE = ConnectionResponsesJson.CreateConnectionResponse.jsonProvider
        val GET_CONNECTIONS_NO_PAGING_REQUEST = ConnectionRequestsJson.validEmptyBody
        val GET_CONNECTIONS_WITH_PAGING_REQUEST = ConnectionRequestsJson.validPagingState
        val GET_CONNECTION_STATUS_REQUEST = ConnectionRequestsJson.validConnectionStatusUpdate
    }
}
