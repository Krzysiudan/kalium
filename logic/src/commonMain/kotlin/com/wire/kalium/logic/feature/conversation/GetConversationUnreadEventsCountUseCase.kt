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

import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.data.conversation.ConversationRepository
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.functional.fold

/**
 * UseCase for getting once the amount of unread events (all: messages, pings, missed calls, etc.) in a specific conversation.
 */
interface GetConversationUnreadEventsCountUseCase {

    suspend operator fun invoke(conversationId: ConversationId): Result

    sealed class Result {
        data class Success(val amount: Long) : Result()
        data class Failure(val storageFailure: StorageFailure) : Result()
    }
}

class GetConversationUnreadEventsCountUseCaseImpl(
    private val conversationRepository: ConversationRepository
) : GetConversationUnreadEventsCountUseCase {

    override suspend fun invoke(conversationId: ConversationId): GetConversationUnreadEventsCountUseCase.Result =
        conversationRepository.getConversationUnreadEventsCount(conversationId).fold(
            { GetConversationUnreadEventsCountUseCase.Result.Failure(it) },
            { GetConversationUnreadEventsCountUseCase.Result.Success(it) }
        )
}
