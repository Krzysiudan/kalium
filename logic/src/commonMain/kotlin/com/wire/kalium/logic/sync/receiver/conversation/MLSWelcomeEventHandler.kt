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

package com.wire.kalium.logic.sync.receiver.conversation

import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.data.client.MLSClientProvider
import com.wire.kalium.logic.data.event.Event
import com.wire.kalium.logic.data.event.EventLoggingStatus
import com.wire.kalium.logic.data.event.logEventProcessing
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.kaliumLogger
import com.wire.kalium.logic.wrapMLSRequest
import com.wire.kalium.logic.wrapStorageRequest
import com.wire.kalium.persistence.dao.ConversationDAO
import com.wire.kalium.persistence.dao.ConversationEntity
import io.ktor.util.decodeBase64Bytes
import kotlinx.coroutines.flow.first

interface MLSWelcomeEventHandler {
    suspend fun handle(event: Event.Conversation.MLSWelcome)
}

internal class MLSWelcomeEventHandlerImpl(
    val mlsClientProvider: MLSClientProvider,
    val conversationDAO: ConversationDAO
) : MLSWelcomeEventHandler {
    override suspend fun handle(event: Event.Conversation.MLSWelcome) {
        mlsClientProvider
            .getMLSClient()
            .flatMap { client ->
                wrapMLSRequest { client.processWelcomeMessage(event.message.decodeBase64Bytes()) }
                    .flatMap { groupID ->

                        var infoLogPair = Pair("info", "Created MLS group from welcome message")
                        val groupIdLogPair = Pair("groupId", groupID.obfuscateId())

                        wrapStorageRequest {
                            if (conversationDAO.getConversationByGroupID(groupID).first() != null) {
                                // Welcome arrived after the conversation create event, updating existing conversation.
                                conversationDAO.updateConversationGroupState(ConversationEntity.GroupState.ESTABLISHED, groupID)
                                infoLogPair = Pair("info", "Updated conversation from welcome message")
                            }
                            kaliumLogger
                                .logEventProcessing(
                                    EventLoggingStatus.SUCCESS,
                                    event,
                                    infoLogPair,
                                    groupIdLogPair
                                )
                        }
                    }
            }
    }
}
