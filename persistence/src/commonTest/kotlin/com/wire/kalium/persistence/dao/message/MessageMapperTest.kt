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
package com.wire.kalium.persistence.dao.message

import com.wire.kalium.persistence.dao.ConversationEntity
import com.wire.kalium.persistence.dao.ConversationIDEntity
import com.wire.kalium.persistence.dao.QualifiedIDEntity
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MessageMapperTest {

    @Test
    fun givenEphemeralOneOnOneConversation_whenMappingToMessagePreviewEntity_thenMessagePreviewEntityContentIsEphemeral() {
        val messagePreviewEntity = Arrangement().toPreviewEntity(ConversationEntity.Type.GROUP, true)

        val content = messagePreviewEntity.content
        assertIs<MessagePreviewEntityContent.Ephemeral>(content)
        assertTrue(content.isGroupConversation)
    }

    @Test
    fun givenEphemeralGroupConversation_whenMappingToMessagePreviewEntity_thenMessagePreviewEntityContentIsEphemeral() {
        val messagePreviewEntity = Arrangement().toPreviewEntity(ConversationEntity.Type.ONE_ON_ONE, true)

        val content = messagePreviewEntity.content
        assertIs<MessagePreviewEntityContent.Ephemeral>(content)
        assertTrue(!content.isGroupConversation)
    }

    private class Arrangement {
        fun toPreviewEntity(conversationType: ConversationEntity.Type, isEphemeral: Boolean): MessagePreviewEntity {
            return MessageMapper.toPreviewEntity(
                id = "someId",
                conversationId = ConversationIDEntity("someValue", "someDomain"),
                contentType = MessageEntity.ContentType.TEXT,
                date = Instant.DISTANT_FUTURE,
                visibility = MessageEntity.Visibility.VISIBLE,
                senderUserId = QualifiedIDEntity("someValue", "someDomain"),
                isEphemeral = isEphemeral,
                senderName = "someName",
                senderConnectionStatus = null,
                senderIsDeleted = null,
                selfUserId = null,
                isSelfMessage = false,
                memberChangeList = listOf(),
                memberChangeType = null,
                updatedConversationName = null,
                conversationName = null,
                isMentioningSelfUser = false,
                isQuotingSelfUser = null,
                text = null,
                assetMimeType = null,
                isUnread = false,
                isNotified = 0,
                mutedStatus = null,
                conversationType = conversationType
            )
        }
    }
}
