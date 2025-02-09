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
package com.wire.kalium.logic.feature.conversation

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.client.MLSClientProvider
import com.wire.kalium.logic.data.conversation.SubconversationRepository
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.GroupID
import com.wire.kalium.logic.data.id.SubconversationId
import com.wire.kalium.logic.data.id.toApi
import com.wire.kalium.logic.data.id.toCrypto
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.CurrentClientIdProvider
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.wrapApiRequest
import com.wire.kalium.logic.wrapCryptoRequest
import com.wire.kalium.network.api.base.authenticated.conversation.ConversationApi
import com.wire.kalium.network.api.base.authenticated.conversation.SubconversationMember

/**
 * Leave a sub-conversation you've previously joined
 */
interface LeaveSubconversationUseCase {
    suspend operator fun invoke(conversationId: ConversationId, subconversationId: SubconversationId): Either<CoreFailure, Unit>
}

class LeaveSubconversationUseCaseImpl(
    val conversationApi: ConversationApi,
    val mlsClientProvider: MLSClientProvider,
    val subconversationRepository: SubconversationRepository,
    val selfUserId: UserId,
    val selfClientIdProvider: CurrentClientIdProvider
) : LeaveSubconversationUseCase {
    override suspend fun invoke(conversationId: ConversationId, subconversationId: SubconversationId): Either<CoreFailure, Unit> =
        retrieveSubconversationGroupId(conversationId, subconversationId).flatMap { groupId ->
            groupId?.let { groupId ->
                wrapApiRequest {
                    conversationApi.leaveSubconversation(conversationId.toApi(), subconversationId.toApi())
                }.flatMap {
                    subconversationRepository.deleteSubconversation(conversationId, subconversationId)
                    mlsClientProvider.getMLSClient().flatMap { mlsClient ->
                        wrapCryptoRequest {
                            mlsClient.wipeConversation(groupId.toCrypto())
                        }
                    }
                }
            } ?: Either.Right(Unit)
        }

    suspend fun retrieveSubconversationGroupId(
        conversationId: ConversationId,
        subconversationId: SubconversationId
    ): Either<CoreFailure, GroupID?> =
        selfClientIdProvider().flatMap { selfClientId ->
            subconversationRepository.getSubconversationInfo(conversationId, subconversationId)?.let {
                Either.Right(it)
            } ?: wrapApiRequest { conversationApi.fetchSubconversationDetails(conversationId.toApi(), subconversationId.toApi()) }.flatMap {
                if (it.members.contains(SubconversationMember(selfClientId.value, selfUserId.value, selfUserId.domain))) {
                    Either.Right(GroupID(it.groupId))
                } else {
                    Either.Right(null)
                }
            }
        }
}
