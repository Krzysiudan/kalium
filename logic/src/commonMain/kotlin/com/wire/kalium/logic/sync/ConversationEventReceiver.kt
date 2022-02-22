package com.wire.kalium.logic.sync

import com.wire.kalium.cryptography.CryptoClientId
import com.wire.kalium.cryptography.CryptoSessionId
import com.wire.kalium.cryptography.ProteusClient
import com.wire.kalium.cryptography.UserId
import com.wire.kalium.cryptography.exceptions.ProteusException
import com.wire.kalium.logic.data.event.Event
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageRepository
import com.wire.kalium.logic.data.message.PlainMessageBlob
import com.wire.kalium.logic.data.message.ProtoContentMapper
import com.wire.kalium.logic.util.Base64
import io.ktor.utils.io.core.toByteArray

class ConversationEventReceiver(
    private val proteusClient: ProteusClient,
    private val messageRepository: MessageRepository,
    private val protoContentMapper: ProtoContentMapper
) : EventReceiver<Event.Conversation> {
    override suspend fun onEvent(event: Event.Conversation) {
        when (event) {
            is Event.Conversation.NewMessage -> handleNewMessage(event)
        }
    }

    private suspend fun handleNewMessage(event: Event.Conversation.NewMessage) {
        val decodedContentBytes = Base64.decodeFromBase64(event.content.toByteArray())

        //TODO Use domain when creating CryptoSession too
        val cryptoSessionId = CryptoSessionId(UserId(event.senderUserId.value), CryptoClientId(event.senderClientId.value))
        val decryptedContentBytes = try {
            proteusClient.decrypt(decodedContentBytes, cryptoSessionId)
        } catch (e: ProteusException) {
            e.printStackTrace()
            //TODO: Insert a failed message into the database to notify user that encryption is kaputt
            return
        }
        val protoContent = protoContentMapper.decodeFromProtobuf(PlainMessageBlob(decryptedContentBytes))

        val message = Message(
            protoContent.messageUid,
            protoContent.messageContent,
            event.conversationId,
            event.time,
            event.senderUserId,
            event.senderClientId,
            Message.Status.SENT
        )

        //TODO Multiplatform logging
        println("Message received: $message")
        messageRepository.persistMessage(message)
    }
}
