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

package com.wire.kalium.api.v0.teams

import com.wire.kalium.api.ApiTest
import com.wire.kalium.api.json.model.ErrorResponseJson
import com.wire.kalium.model.ServiceDetailsResponseJson
import com.wire.kalium.model.TeamsResponsesJson
import com.wire.kalium.network.api.base.authenticated.TeamsApi
import com.wire.kalium.network.api.base.model.ErrorResponse
import com.wire.kalium.network.api.base.model.ServiceDetailResponse
import com.wire.kalium.network.api.v0.authenticated.TeamsApiV0
import com.wire.kalium.network.exceptions.KaliumException
import com.wire.kalium.network.utils.NetworkResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@ExperimentalCoroutinesApi
internal class TeamsApiV0Test : ApiTest() {

    @Test
    fun givenAValidGetTeamsFirstPageRequest_whenGettingTeamsMembers_theRequestShouldBeConfiguredCorrectly() =
        runTest {
            val networkClient = mockAuthenticatedNetworkClient(
                GET_TEAM_MEMBER_CLIENT_RESPONSE.rawJson,
                statusCode = HttpStatusCode.OK,
                assertion = {
                    assertGet()
                    assertQueryExist("maxResults")
                    assertQueryParameter("maxResults", hasValue = "10")
                    assertPathEqual("/$PATH_TEAMS/$DUMMY_TEAM_ID/$PATH_MEMBERS")
                }
            )
            val teamsApi: TeamsApi = TeamsApiV0(networkClient)
            teamsApi.getTeamMembers(DUMMY_TEAM_ID, limitTo = 10)
        }

    @Test
    fun givenADeleteConversationForTeamRequest_whenDeletingATeamConversationWithSuccess_theRequestShouldBeConfiguredCorrectly() =
        runTest {
            val conversationId = "96a6e8e4-6420-49db-aa83-2711edf7580d"
            val networkClient = mockAuthenticatedNetworkClient(
                "",
                statusCode = HttpStatusCode.OK,
                assertion = {
                    assertDelete()
                    assertPathEqual("/$PATH_TEAMS/$DUMMY_TEAM_ID/$PATH_CONVERSATIONS/$conversationId")
                }
            )
            val teamsApi: TeamsApi = TeamsApiV0(networkClient)
            teamsApi.deleteConversation(conversationId, teamId = DUMMY_TEAM_ID)
        }

    @Test
    fun givenAValidWhitelistedServicesRequest_whenGettingWhitelistedServices_theRequestShouldBeConfiguredCorrectly() =
        runTest {
            val networkClient = mockAuthenticatedNetworkClient(
                SERVICES_LIST_RESPONSE.rawJson,
                statusCode = HttpStatusCode.OK,
                assertion = {
                    assertGet()
                    assertPathEqual("/$PATH_TEAMS/$DUMMY_TEAM_ID/services/whitelisted")
                    assertQueryParameter("size", "2")
                }
            )
            val teamsApi: TeamsApi = TeamsApiV0(networkClient)
            teamsApi.whiteListedServices(DUMMY_TEAM_ID, 2).also {
                assertIs<NetworkResponse.Success<ServiceDetailResponse>>(it)
                assertEquals(SERVICES_LIST_RESPONSE.serializableData, it.value)
            }
        }

    @Test
    fun givenInvalidTeamId_whenGettingWhitelistedServices_theRequestShouldBeConfiguredCorrectly() =
        runTest {
            val errorResponse = ErrorResponseJson.valid(
                ErrorResponse(
                    code = HttpStatusCode.NotFound.value,
                    message = "Team not found",
                    label = "team.not.found"
                )
            )
            val networkClient = mockAuthenticatedNetworkClient(
                errorResponse.rawJson,
                statusCode = HttpStatusCode.NotFound,
                assertion = {
                    assertGet()
                    assertPathEqual("/$PATH_TEAMS/$DUMMY_TEAM_ID/services/whitelisted")
                    assertQueryParameter("size", "2")
                }
            )
            val teamsApi: TeamsApi = TeamsApiV0(networkClient)
            teamsApi.whiteListedServices(DUMMY_TEAM_ID, 2).also {
                assertIs<NetworkResponse.Error>(it)
                assertIs<KaliumException.InvalidRequestError>(it.kException)
                assertEquals(
                    HttpStatusCode.NotFound.value,
                    (it.kException as KaliumException.InvalidRequestError).errorResponse.code
                )
            }
        }

    private companion object {
        const val PATH_TEAMS = "teams"
        const val PATH_CONVERSATIONS = "conversations"
        const val PATH_MEMBERS = "members"
        const val DUMMY_TEAM_ID = "770b0623-ffd5-4e08-8092-7a6b9b9ca3b4"
        val GET_TEAM_MEMBER_CLIENT_RESPONSE = TeamsResponsesJson.GetTeamsMembers.validGetTeamsMembers
        val SERVICES_LIST_RESPONSE = ServiceDetailsResponseJson.valid
    }
}
