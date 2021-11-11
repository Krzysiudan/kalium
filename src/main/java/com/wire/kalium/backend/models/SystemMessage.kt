package com.wire.kalium.backend.models

import com.wire.kalium.tools.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SystemMessage(
        @Serializable(with = UUIDSerializer::class)  val id: UUID,
    val type: String,
    val time: String,
    @Serializable(with = UUIDSerializer::class)  val from: UUID,
    val conversation: Conversation,
    @Serializable(with = UUIDSerializer::class)  val convId: UUID,
    val userIds: List<@Serializable(with = UUIDSerializer::class) UUID>
)
