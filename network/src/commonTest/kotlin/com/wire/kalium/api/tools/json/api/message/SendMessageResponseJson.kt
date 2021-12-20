package com.wire.kalium.api.tools.json.api.message

import com.wire.kalium.api.tools.json.ValidJsonProvider
import com.wire.kalium.network.api.message.SendMessageResponse
import com.wire.kalium.network.api.message.UserIdToClientMap

object SendMessageResponseJson {

    private const val TIME = "2021-05-31T10:52:02.671Z"

    private const val USER_1 = "user10d0-000b-9c1a-000d-a4130002c221"
    private const val USER_1_client_1 = "60f85e4b15ad3786"
    private const val USER_1_client_2 = "6e323ab31554353b"

    private const val USER_2 = "user200d0-000b-9c1a-000d-a4130002c221"
    private const val USER_2_client_1 = "32233lj33j3dfh7u"
    private const val USER_2_client_2 = "duf3eif09324wq5j"

    private val USER_MAP = mapOf(
        USER_1 to listOf(USER_1_client_1, USER_1_client_2),
        USER_2 to listOf(USER_2_client_1, USER_2_client_2)
    )

    private val emptyResponse = { _: SendMessageResponse ->
        """
        |{
        |   "time": "$TIME"
        |   "missing" : {},
        |   "redundant" : {},
        |   "deleted" : {}
        |}
        """.trimMargin()
    }

    private val missingProvider = { _: SendMessageResponse ->
        """
            |{
            |   "time": "$TIME"
            |   "missing" : {
            |       "$USER_1": [
            |           "$USER_1_client_1",
            |           "$USER_1_client_2"
            |        ],
            |       "$USER_2": [
            |           "$USER_2_client_1",
            |           "$USER_2_client_2"
            |       ]
            |   },
            |   "redundant" : {},
            |   "deleted" : {}
            |}
        """.trimMargin()
    }

    private val redundantProvider = { _: SendMessageResponse ->
        """
            |{
            |   "time": "$TIME"
            |   "missing" : {},
            |   "redundant" : {
            |       "$USER_1": [
            |           "$USER_1_client_1",
            |           "$USER_1_client_2"
            |        ],
            |       "$USER_2": [
            |           "$USER_2_client_1",
            |           "$USER_2_client_2"
            |       ]
            |   },
            |   "deleted" : {}
            |}
        """.trimMargin()
    }

    private val deletedProvider = { _: SendMessageResponse ->
        """
            |{
            |   "time": "$TIME"
            |   "missing" : {},
            |   "redundant" : {},
            |   "deleted" : {
            |       "$USER_1": [
            |           "$USER_1_client_1",
            |           "$USER_1_client_2"
            |        ],
            |       "$USER_2": [
            |           "$USER_2_client_1",
            |           "$USER_2_client_2"
            |       ]
            |   }
            |}
        """.trimMargin()
    }

    val validMessageSentJson = ValidJsonProvider(
        SendMessageResponse.MessageSent(TIME, mapOf(), mapOf(), mapOf()),
        emptyResponse
    )

    val missingUsersResponse = ValidJsonProvider(
        SendMessageResponse.MissingDevicesResponse(TIME, missing = USER_MAP, mapOf(), mapOf()),
        missingProvider
    )

    val deletedUsersResponse = ValidJsonProvider(
        SendMessageResponse.MissingDevicesResponse(TIME, mapOf(), mapOf(), deleted = USER_MAP),
        deletedProvider
    )

    val redundantUsersResponse = ValidJsonProvider(
        SendMessageResponse.MissingDevicesResponse(TIME, mapOf(), redundant = USER_MAP, mapOf()),
        redundantProvider
    )
}
