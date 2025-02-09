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

package com.wire.kalium.logic.feature.call

import com.sun.jna.Pointer
import com.wire.kalium.calling.CallTypeCalling
import com.wire.kalium.calling.Calling
import com.wire.kalium.calling.callbacks.ConstantBitRateStateChangeHandler
import com.wire.kalium.calling.callbacks.MetricsHandler
import com.wire.kalium.calling.callbacks.ReadyHandler
import com.wire.kalium.calling.types.Handle
import com.wire.kalium.calling.types.Uint32_t
import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.cache.SelfConversationIdProvider
import com.wire.kalium.logic.callingLogger
import com.wire.kalium.logic.data.call.CallClient
import com.wire.kalium.logic.data.call.CallClientList
import com.wire.kalium.logic.data.call.CallRepository
import com.wire.kalium.logic.data.call.CallType
import com.wire.kalium.logic.data.call.ConversationType
import com.wire.kalium.logic.data.call.EpochInfo
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.data.call.VideoStateChecker
import com.wire.kalium.logic.data.call.mapper.CallMapper
import com.wire.kalium.logic.data.call.mapper.ParticipantMapperImpl
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationRepository
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.FederatedIdMapper
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.UserRepository
import com.wire.kalium.logic.feature.CurrentClientIdProvider
import com.wire.kalium.logic.feature.call.scenario.OnActiveSpeakers
import com.wire.kalium.logic.feature.call.scenario.OnAnsweredCall
import com.wire.kalium.logic.feature.call.scenario.OnClientsRequest
import com.wire.kalium.logic.feature.call.scenario.OnCloseCall
import com.wire.kalium.logic.feature.call.scenario.OnConfigRequest
import com.wire.kalium.logic.feature.call.scenario.OnEstablishedCall
import com.wire.kalium.logic.feature.call.scenario.OnIncomingCall
import com.wire.kalium.logic.feature.call.scenario.OnMissedCall
import com.wire.kalium.logic.feature.call.scenario.OnNetworkQualityChanged
import com.wire.kalium.logic.feature.call.scenario.OnParticipantListChanged
import com.wire.kalium.logic.feature.call.scenario.OnParticipantsVideoStateChanged
import com.wire.kalium.logic.feature.call.scenario.OnRequestNewEpoch
import com.wire.kalium.logic.feature.call.scenario.OnSFTRequest
import com.wire.kalium.logic.feature.call.scenario.OnSendOTR
import com.wire.kalium.logic.feature.message.MessageSender
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import com.wire.kalium.logic.functional.fold
import com.wire.kalium.logic.util.toInt
import com.wire.kalium.util.DateTimeUtil.toEpochMillis
import com.wire.kalium.util.KaliumDispatcher
import com.wire.kalium.util.KaliumDispatcherImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Collections

@Suppress("LongParameterList", "TooManyFunctions")
class CallManagerImpl internal constructor(
    private val calling: Calling,
    private val callRepository: CallRepository,
    private val userRepository: UserRepository,
    private val currentClientIdProvider: CurrentClientIdProvider,
    private val selfConversationIdProvider: SelfConversationIdProvider,
    private val conversationRepository: ConversationRepository,
    private val messageSender: MessageSender,
    private val callMapper: CallMapper,
    private val federatedIdMapper: FederatedIdMapper,
    private val qualifiedIdMapper: QualifiedIdMapper,
    private val videoStateChecker: VideoStateChecker,
    private val kaliumConfigs: KaliumConfigs,
    kaliumDispatchers: KaliumDispatcher = KaliumDispatcherImpl
) : CallManager {

    private val job = SupervisorJob() // TODO(calling): clear job method
    private val scope = CoroutineScope(job + kaliumDispatchers.io)
    private val deferredHandle: Deferred<Handle> = startHandleAsync()

    private val strongReferences = Collections.synchronizedList(mutableListOf<Any>())
    private fun <T : Any> T.keepingStrongReference(): T {
        strongReferences.add(this)
        return this
    }

    private val clientId: Deferred<ClientId> = scope.async(start = CoroutineStart.LAZY) {
        currentClientIdProvider().fold({
            TODO("adjust correct variable calling")
        }, {
            callingLogger.d("$TAG - clientId $it")
            it
        })
    }
    private val userId: Deferred<UserId> = scope.async(start = CoroutineStart.LAZY) {
        userRepository.observeSelfUser().first().id.also {
            callingLogger.d("$TAG - userId ${it.toLogString()}")
        }
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private val metricsHandler = MetricsHandler { conversationId: String, metricsJson: String, arg: Pointer? ->
        callingLogger.i("$TAG -> metricsHandler")
    }.keepingStrongReference()

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private val constantBitRateStateChangeHandler =
        ConstantBitRateStateChangeHandler { userId: String, clientId: String, isEnabled: Boolean, arg: Pointer? ->
            callingLogger.i(
                "$TAG -> constantBitRateStateChangeHandler for userId: ${userId.obfuscateId()} " +
                        "clientId: ${clientId.obfuscateId()}  isCbrEnabled: $isEnabled"
            )
            callRepository.updateIsCbrEnabled(isEnabled)
        }.keepingStrongReference()

    private fun startHandleAsync(): Deferred<Handle> {
        return scope.async(start = CoroutineStart.LAZY) {
            val selfUserId = federatedIdMapper.parseToFederatedId(userId.await())
            val selfClientId = clientId.await().value

            val waitInitializationJob = Job()

            val handle = calling.wcall_create(
                userId = selfUserId,
                clientId = selfClientId,
                readyHandler = ReadyHandler { version: Int, arg: Pointer? ->
                    callingLogger.i("$TAG -> readyHandler; version=$version; arg=$arg")
                    onCallingReady()
                    waitInitializationJob.complete()
                    Unit
                }.keepingStrongReference(),
                // TODO(refactor): inject all of these CallbackHandlers in class constructor
                sendHandler = OnSendOTR(
                    deferredHandle,
                    calling,
                    qualifiedIdMapper,
                    selfUserId,
                    selfClientId,
                    messageSender,
                    selfConversationIdProvider,
                    scope,
                    callMapper
                ).keepingStrongReference(),
                sftRequestHandler = OnSFTRequest(deferredHandle, calling, callRepository, scope)
                    .keepingStrongReference(),
                incomingCallHandler = OnIncomingCall(callRepository, callMapper, qualifiedIdMapper, scope, kaliumConfigs)
                    .keepingStrongReference(),
                missedCallHandler = OnMissedCall,
                answeredCallHandler = OnAnsweredCall(callRepository, scope, qualifiedIdMapper)
                    .keepingStrongReference(),
                establishedCallHandler = OnEstablishedCall(callRepository, scope, qualifiedIdMapper)
                    .keepingStrongReference(),
                closeCallHandler = OnCloseCall(callRepository, scope, qualifiedIdMapper)
                    .keepingStrongReference(),
                metricsHandler = metricsHandler,
                callConfigRequestHandler = OnConfigRequest(calling, callRepository, scope)
                    .keepingStrongReference(),
                constantBitRateStateChangeHandler = constantBitRateStateChangeHandler,
                videoReceiveStateHandler = OnParticipantsVideoStateChanged(),
                arg = null
            )
            callingLogger.d("$TAG - wcall_create() called")
            // TODO(edge-case): Add a timeout. Perhaps make some functions (like onCallingMessageReceived) return Eithers.
            waitInitializationJob.join()
            handle
        }
    }

    private suspend fun <T> withCalling(action: suspend Calling.(handle: Handle) -> T): T {
        val handle = deferredHandle.await()
        return calling.action(handle)
    }

    override suspend fun onCallingMessageReceived(
        message: Message.Signaling,
        content: MessageContent.Calling,
    ) = withCalling {
        callingLogger.i("$TAG - onCallingMessageReceived called: { \"message\" : ${message.toLogString()}}")
        val msg = content.value.toByteArray()

        val currTime = System.currentTimeMillis()
        val msgTime = message.date.toEpochMillis()

        val targetConversationId = if (message.isSelfMessage) {
            content.conversationId ?: message.conversationId
        } else {
            message.conversationId
        }

        val type = conversationRepository.getConversationById(targetConversationId)?.let {
            callMapper.fromConversationToConversationType(it)
        } ?: ConversationType.Unknown

        wcall_recv_msg(
            inst = deferredHandle.await(),
            msg = msg,
            len = msg.size,
            curr_time = Uint32_t(value = currTime / 1000),
            msg_time = Uint32_t(value = msgTime / 1000),
            convId = federatedIdMapper.parseToFederatedId(targetConversationId),
            userId = federatedIdMapper.parseToFederatedId(message.senderUserId),
            clientId = message.senderClientId.value,
            convType = callMapper.toConversationTypeCalling(type).avsValue
        )
        callingLogger.i("$TAG - wcall_recv_msg() called")
    }

    override suspend fun startCall(
        conversationId: ConversationId,
        callType: CallType,
        isAudioCbr: Boolean
    ) {
        callingLogger.d(
            "$TAG -> starting call for conversation = " +
                    "${conversationId.toLogString()}.."
        )
        val isCameraOn = callType == CallType.VIDEO
        val type = conversationRepository.getConversationById(conversationId)?.let {
            callMapper.fromConversationToConversationType(it)
        } ?: ConversationType.Unknown

        callRepository.createCall(
            conversationId = conversationId,
            type = type,
            status = CallStatus.STARTED,
            isMuted = false,
            isCameraOn = isCameraOn,
            isCbrEnabled = isAudioCbr,
            callerId = userId.await().toString()
        )

        withCalling {
            val avsCallType = callMapper.toCallTypeCalling(callType)
            val avsConversationType = callMapper.toConversationTypeCalling(type)
            // TODO: Handle response. Possible failure?
            wcall_start(
                deferredHandle.await(),
                federatedIdMapper.parseToFederatedId(conversationId),
                avsCallType.avsValue,
                avsConversationType.avsValue,
                isAudioCbr.toInt()
            )

            callingLogger.d(
                "$TAG - wcall_start() called -> Call for conversation = " +
                        "${conversationId.toLogString()} started"
            )
        }

        if (callRepository.getCallMetadataProfile()[conversationId]?.protocol is Conversation.ProtocolInfo.MLS) {
            callRepository.joinMlsConference(conversationId) { conversationId, epochInfo ->
                updateEpochInfo(conversationId, epochInfo)
            }
        }
    }

    override suspend fun answerCall(
        conversationId: ConversationId,
        isAudioCbr: Boolean
    ) {
        withCalling {
            callingLogger.d(
                "$TAG -> answering call for conversation = " +
                        "${conversationId.toLogString()}.."
            )
            wcall_answer(
                inst = deferredHandle.await(),
                conversationId = federatedIdMapper.parseToFederatedId(conversationId),
                callType = CallTypeCalling.AUDIO.avsValue,
                cbrEnabled = isAudioCbr
            )
            callingLogger.d(
                "$TAG - wcall_answer() called -> Incoming call for conversation = " +
                        "${conversationId.toLogString()} answered"
            )
        }

        if (callRepository.getCallMetadataProfile()[conversationId]?.protocol is Conversation.ProtocolInfo.MLS) {
            callRepository.joinMlsConference(conversationId) { conversationId, epochInfo ->
                updateEpochInfo(conversationId, epochInfo)
            }
        }
    }

    override suspend fun endCall(conversationId: ConversationId) = withCalling {
        callingLogger.d(
            "[$TAG][endCall] -> ConversationId: " +
                    "[${conversationId.toLogString()}]"
        )
        callingLogger.d("[$TAG][endCall] -> Calling wcall_end()")
        wcall_end(
            inst = deferredHandle.await(),
            conversationId = federatedIdMapper.parseToFederatedId(conversationId)
        )
    }

    override suspend fun rejectCall(conversationId: ConversationId) = withCalling {
        callingLogger.d(
            "[$TAG][rejectCall] -> ConversationId: " +
                    "[${conversationId.toLogString()}]"
        )
        callingLogger.d("[$TAG][rejectCall] -> Calling wcall_reject()")
        wcall_reject(
            inst = deferredHandle.await(),
            conversationId = federatedIdMapper.parseToFederatedId(conversationId)
        )
    }

    override suspend fun muteCall(shouldMute: Boolean) = withCalling {
        val logString = if (shouldMute) "muting" else "un-muting"
        callingLogger.d("$TAG -> $logString call..")
        wcall_set_mute(deferredHandle.await(), muted = shouldMute.toInt())
        callingLogger.d("$TAG - wcall_set_mute() called")
    }

    /**
     * This method should NOT be called while the call is still incoming or outgoing and not established yet.
     */
    override suspend fun updateVideoState(conversationId: ConversationId, videoState: VideoState) {
        withCalling {
            callingLogger.d("$TAG -> changing video state to ${videoState.name}..")
            scope.launch {
                val videoStateCalling = callMapper.toVideoStateCalling(videoState)
                wcall_set_video_send_state(
                    deferredHandle.await(),
                    federatedIdMapper.parseToFederatedId(conversationId),
                    videoStateCalling.avsValue
                )
                callingLogger.d("$TAG -> wcall_set_video_send_state called..")
            }
        }
    }

    override suspend fun requestVideoStreams(conversationId: ConversationId, callClients: CallClientList) {
        withCalling {
            // Needed to support calls between federated and non federated environments
            val clients = callClients.clients.map { callClient ->
                CallClient(
                    federatedIdMapper.parseToFederatedId(callClient.userId),
                    callClient.clientId
                )
            }
            val clientsJson = CallClientList(clients).toJsonString()
            val conversationIdString = federatedIdMapper.parseToFederatedId(conversationId)
            calling.wcall_request_video_streams(
                inst = it,
                conversationId = conversationIdString,
                mode = DEFAULT_REQUEST_VIDEO_STREAMS_MODE,
                json = clientsJson
            )
        }
    }

    override suspend fun updateEpochInfo(conversationId: ConversationId, epochInfo: EpochInfo) {
        withCalling {
            callingLogger.d(
                "$TAG - wcall_set_epoch_info() called -> Updating epoch info call for conversation = " +
                        "${conversationId.toLogString()} for epoch = ${epochInfo.epoch}"
            )

            wcall_set_epoch_info(
                it,
                federatedIdMapper.parseToFederatedId(conversationId),
                Uint32_t(epochInfo.epoch.toLong()),
                epochInfo.members.toJsonString(),
                epochInfo.sharedSecret,
                Uint32_t(epochInfo.sharedSecret.size.toLong())
            )
        }
    }

    /**
     * onCallingReady
     * Will start the handlers for: ParticipantsChanged, NetworkQuality, ClientsRequest and ActiveSpeaker
     */
    private fun onCallingReady() {
        initParticipantsHandler()
        initNetworkHandler()
        initClientsHandler()
        initActiveSpeakersHandler()
        initRequestNewEpochHandler()
    }

    private fun initParticipantsHandler() {
        scope.launch {
            withCalling {
                val onParticipantListChanged = OnParticipantListChanged(
                    callRepository = callRepository,
                    qualifiedIdMapper = qualifiedIdMapper,
                    participantMapper = ParticipantMapperImpl(videoStateChecker, callMapper),
                    userRepository = userRepository,
                    callingScope = scope
                ).keepingStrongReference()

                wcall_set_participant_changed_handler(
                    inst = deferredHandle.await(),
                    wcall_participant_changed_h = onParticipantListChanged,
                    arg = null
                )
                callingLogger.d("$TAG - wcall_set_participant_changed_handler() called")
            }
        }
    }

    private fun initNetworkHandler() {
        scope.launch {
            withCalling {
                val onNetworkQualityChanged = OnNetworkQualityChanged()
                    .keepingStrongReference()

                wcall_set_network_quality_handler(
                    inst = deferredHandle.await(),
                    wcall_network_quality_h = onNetworkQualityChanged,
                    intervalInSeconds = NETWORK_QUALITY_INTERVAL_SECONDS,
                    arg = null
                )
                callingLogger.d("$TAG - wcall_set_network_quality_handler() called")
            }
        }
    }

    private fun initClientsHandler() {
        scope.launch {
            withCalling {
                val onClientsRequest = OnClientsRequest(
                    calling = calling,
                    conversationRepository = conversationRepository,
                    federatedIdMapper = federatedIdMapper,
                    qualifiedIdMapper = qualifiedIdMapper,
                    callingScope = scope
                ).keepingStrongReference()

                wcall_set_req_clients_handler(
                    inst = deferredHandle.await(),
                    wcall_req_clients_h = onClientsRequest
                )

                callingLogger.d("$TAG - wcall_set_req_clients_handler() called")
            }
        }
    }

    private fun initActiveSpeakersHandler() {
        scope.launch {
            withCalling {
                val activeSpeakersHandler = OnActiveSpeakers(
                    callRepository = callRepository,
                    qualifiedIdMapper = qualifiedIdMapper
                ).keepingStrongReference()

                wcall_set_active_speaker_handler(
                    inst = deferredHandle.await(),
                    activeSpeakersHandler = activeSpeakersHandler
                )

                callingLogger.d("$TAG - wcall_set_req_clients_handler() called")
            }
        }
    }

    private fun initRequestNewEpochHandler() {
        scope.launch {
            withCalling {
                val requestNewEpochHandler = OnRequestNewEpoch(
                    scope = scope,
                    callRepository = callRepository,
                    qualifiedIdMapper = qualifiedIdMapper
                ).keepingStrongReference()

                wcall_set_req_new_epoch_handler(
                    inst = deferredHandle.await(),
                    requestNewEpochHandler = requestNewEpochHandler
                )

                callingLogger.d("$TAG - wcall_set_req_new_epoch_handler() called")
            }
        }
    }

    companion object {
        private const val DEFAULT_REQUEST_VIDEO_STREAMS_MODE = 0
        const val TAG = "CallManager"
        const val NETWORK_QUALITY_INTERVAL_SECONDS = 5
        const val UTF8_ENCODING = "UTF-8"
    }
}
