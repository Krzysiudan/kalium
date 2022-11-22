package com.wire.kalium.logic.feature.conversation

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationGroupRepository
import com.wire.kalium.logic.data.conversation.ConversationRepository
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.functional.fold
import kotlinx.coroutines.flow.first

/**
 * Operation that creates one-to-one Conversation with specific [UserId] (only if it is absent in local DB)
 * and returns [Conversation] data.
 *
 * @param otherUserId [UserId] private conversation with which we are interested in.
 * @return Result with [Conversation] in case of success, or [CoreFailure] if something went wrong:
 * can't get data from local DB, or can't create a conversation.
 */
class GetOrCreateOneToOneConversationUseCase(
    private val conversationRepository: ConversationRepository,
    private val conversationGroupRepository: ConversationGroupRepository
) {

    suspend operator fun invoke(otherUserId: UserId): CreateConversationResult {
        // TODO: filter out self user from the list (just in case of client bug that leads to self user to be included part of the list)
        return conversationRepository.observeOneToOneConversationWithOtherUser(otherUserId)
            .first()
            .fold({ conversationFailure ->
                if (conversationFailure is StorageFailure.DataNotFound) {
                    conversationGroupRepository.createGroupConversation(usersList = listOf(otherUserId))
                        .fold(
                            CreateConversationResult::Failure,
                            CreateConversationResult::Success
                        )
                } else {
                    CreateConversationResult.Failure(conversationFailure)
                }
            }, { conversation ->
                CreateConversationResult.Success(conversation)
            })
    }

}

sealed class CreateConversationResult {
    data class Success(val conversation: Conversation) : CreateConversationResult()
    data class Failure(val coreFailure: CoreFailure) : CreateConversationResult()
}
