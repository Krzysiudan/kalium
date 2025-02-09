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
package com.wire.kalium.logic.feature.message.ephemeral

import com.benasher44.uuid.uuid4
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logger.KaliumLogger.Companion.ApplicationFlow.ASSETS
import com.wire.kalium.logger.KaliumLogger.Companion.ApplicationFlow.MESSAGES
import com.wire.kalium.logic.cache.SelfConversationIdProvider
import com.wire.kalium.logic.data.asset.AssetRepository
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.message.MessageRepository
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.CurrentClientIdProvider
import com.wire.kalium.logic.feature.message.MessageSender
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.functional.foldToEitherWhileRight
import com.wire.kalium.logic.functional.map
import com.wire.kalium.logic.functional.onFailure
import com.wire.kalium.logic.functional.onSuccess
import com.wire.kalium.logic.kaliumLogger
import com.wire.kalium.util.DateTimeUtil

/**
 * When the self user is receiver of the self deletion message,
 * we delete it permanently after expiration and inform the sender by broadcasting a message to delete
 * for the self-deleting message, before the receiver does it on the sender side, the message is simply marked as deleted
 * see [com.wire.kalium.logic.feature.message.ephemeral.DeleteEphemeralMessageForSelfUserAsReceiverUseCaseImpl]
 **/
interface DeleteEphemeralMessageForSelfUserAsReceiverUseCase {
    /**
     * @param conversationId the conversation id that contains the self-deleting message
     * @param messageId the id of the self-deleting message
     */
    suspend operator fun invoke(conversationId: ConversationId, messageId: String): Either<CoreFailure, Unit>
}

internal class DeleteEphemeralMessageForSelfUserAsReceiverUseCaseImpl(
    private val messageRepository: MessageRepository,
    private val assetRepository: AssetRepository,
    private val currentClientIdProvider: CurrentClientIdProvider,
    private val messageSender: MessageSender,
    private val selfUserId: UserId,
    private val selfConversationIdProvider: SelfConversationIdProvider
) : DeleteEphemeralMessageForSelfUserAsReceiverUseCase {
    override suspend fun invoke(conversationId: ConversationId, messageId: String): Either<CoreFailure, Unit> =
        messageRepository.getMessageById(conversationId, messageId).map { message ->
            when (message.status) {
                // TODO: there is a race condition here where a message can still be marked as Message.Status.FAILED but be sent
                // better to send the delete message anyway and let it to other clients to ignore it if the message is not sent
                Message.Status.FAILED -> messageRepository.deleteMessage(messageId, conversationId)
                else -> {
                    currentClientIdProvider().flatMap { currentClientId ->
                        broadCastDeletionToConversation(messageId, conversationId, currentClientId)
                            .flatMap {
                                broadCastDeletionForSelfUser(messageId, conversationId, currentClientId)
                            }
                    }.onSuccess { deleteMessageAssetIfExists(message) }
                        .flatMap { messageRepository.deleteMessage(messageId, conversationId) }
                }
            }.onFailure { failure ->
                kaliumLogger.withFeatureId(MESSAGES).w("delete message failure: $message")
                if (failure is CoreFailure.Unknown) {
                    failure.rootCause?.printStackTrace()
                }
            }
        }

    private suspend fun broadCastDeletionForSelfUser(
        messageId: String,
        conversationId: ConversationId,
        currentClientId: ClientId
    ) = selfConversationIdProvider().flatMap { selfConversationIds ->
        selfConversationIds.foldToEitherWhileRight(Unit) { selfConversationId, _ ->
            val regularMessage = Message.Signaling(
                id = uuid4().toString(),
                content = MessageContent.DeleteForMe(
                    messageId = messageId,
                    conversationId = conversationId
                ),
                conversationId = selfConversationId,
                date = DateTimeUtil.currentIsoDateTimeString(),
                senderUserId = selfUserId,
                senderClientId = currentClientId,
                status = Message.Status.PENDING,
                isSelfMessage = true
            )
            messageSender.sendMessage(regularMessage)
        }
    }

    private suspend fun broadCastDeletionToConversation(
        messageId: String,
        conversationId: ConversationId,
        currentClientId: ClientId
    ): Either<CoreFailure, Unit> {
        val regularMessage = Message.Signaling(
            id = uuid4().toString(),
            content = MessageContent.DeleteMessage(messageId),
            conversationId = conversationId,
            date = DateTimeUtil.currentIsoDateTimeString(),
            senderUserId = selfUserId,
            senderClientId = currentClientId,
            status = Message.Status.PENDING,
            isSelfMessage = false
        )
        return messageSender.sendMessage(regularMessage)
    }

    private suspend fun deleteMessageAssetIfExists(message: Message) {
        (message.content as? MessageContent.Asset)?.value?.remoteData?.let { assetToRemove ->

            assetRepository.deleteAsset(
                assetToRemove.assetId,
                assetToRemove.assetDomain,
                assetToRemove.assetToken
            )
                .onFailure {
                    kaliumLogger.withFeatureId(ASSETS).w("delete message asset failure: $it")
                }
        }
    }
}
