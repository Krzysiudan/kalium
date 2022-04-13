package com.wire.kalium.logic.feature.call.usecase

import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.call.CallManager

class RejectCallUseCase(private val callManager: CallManager) {

    suspend operator fun invoke(conversationId: ConversationId) {
        callManager.rejectCall(conversationId)
    }
}
