package com.wire.kalium.logic.feature.call

import com.wire.kalium.logic.data.call.CallRepository
import com.wire.kalium.logic.data.conversation.ConversationRepository
import com.wire.kalium.logic.data.user.UserRepository
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.GetIncomingCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.GetIncomingCallsUseCaseImpl
import com.wire.kalium.logic.feature.call.usecase.GetOngoingCallUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveSpeakerUseCase
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import com.wire.kalium.logic.feature.call.usecase.SetVideoPreviewUseCase
import com.wire.kalium.logic.feature.call.usecase.StartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOffUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOnUseCase
import com.wire.kalium.logic.feature.call.usecase.UnMuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.UpdateVideoStateUseCase
import com.wire.kalium.logic.sync.SyncManager

@Suppress("LongParameterList")
class CallsScope(
    private val callManager: Lazy<CallManager>,
    private val callRepository: CallRepository,
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val flowManagerService: FlowManagerService,
    private val mediaManagerService: MediaManagerService,
    private val syncManager: SyncManager
) {

    val allCalls: GetAllCallsUseCase
        get() = GetAllCallsUseCase(
            callRepository = callRepository,
            syncManager = syncManager
        )

    val onGoingCall: GetOngoingCallUseCase
        get() = GetOngoingCallUseCase(
            callRepository = callRepository,
            syncManager = syncManager
        )

    val getIncomingCalls: GetIncomingCallsUseCase
        get() = GetIncomingCallsUseCaseImpl(
            callRepository = callRepository,
            syncManager = syncManager,
            conversationRepository = conversationRepository,
            userRepository = userRepository
        )

    val startCall: StartCallUseCase get() = StartCallUseCase(callManager)

    val answerCall: AnswerCallUseCase get() = AnswerCallUseCaseImpl(callManager)

    val endCall: EndCallUseCase get() = EndCallUseCase(callManager)

    val rejectCall: RejectCallUseCase get() = RejectCallUseCase(callManager)

    val muteCall: MuteCallUseCase get() = MuteCallUseCase(callManager, callRepository)

    val unMuteCall: UnMuteCallUseCase get() = UnMuteCallUseCase(callManager, callRepository)

    val updateVideoState: UpdateVideoStateUseCase get() = UpdateVideoStateUseCase(callManager, callRepository)

    val setVideoPreview: SetVideoPreviewUseCase get() = SetVideoPreviewUseCase(flowManagerService)

    val turnLoudSpeakerOff: TurnLoudSpeakerOffUseCase get() = TurnLoudSpeakerOffUseCase(mediaManagerService)

    val turnLoudSpeakerOn: TurnLoudSpeakerOnUseCase get() = TurnLoudSpeakerOnUseCase(mediaManagerService)

    val observeSpeaker: ObserveSpeakerUseCase get() = ObserveSpeakerUseCase(mediaManagerService)
}
