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

import com.wire.kalium.logic.data.call.CallRepository
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.call.usecase.UpdateVideoStateUseCase
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.eq
import io.mockative.given
import io.mockative.mock
import io.mockative.once
import io.mockative.thenDoNothing
import io.mockative.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class UpdateVideoStateUseCaseTest {

    @Mock
    private val callManager = mock(classOf<CallManager>())

    @Mock
    private val callRepository = mock(classOf<CallRepository>())

    private lateinit var updateVideoStateUseCase: UpdateVideoStateUseCase

    @BeforeTest
    fun setup() {
        updateVideoStateUseCase = UpdateVideoStateUseCase(lazy { callManager }, callRepository)
        given(callRepository)
            .function(callRepository::updateIsCameraOnById)
            .whenInvokedWith(eq(conversationId.toString()), eq(isCameraOn))
            .thenDoNothing()

        given(callManager)
            .suspendFunction(callManager::updateVideoState)
            .whenInvokedWith(any(), any())
            .thenReturn(Unit)
    }

    @Test
    fun givenAFlowOfEstablishedCallsThatContainsAnEstablishedCall_whenUseCaseInvoked_thenInvokeUpdateVideoState() = runTest {
        val establishedCall = Call(
            conversationId,
            CallStatus.ESTABLISHED,
            isMuted = true,
            isCameraOn = true,
            isCbrEnabled = false,
            callerId = "caller-id",
            conversationName = "",
            Conversation.Type.ONE_ON_ONE,
            null,
            null
        )
        given(callManager)
            .suspendFunction(callManager::updateVideoState)
            .whenInvokedWith(eq(conversationId), eq(videoState))
            .thenDoNothing()

        given(callRepository)
            .suspendFunction(callRepository::establishedCallsFlow)
            .whenInvoked().then {
                flowOf(listOf(establishedCall))
            }
        given(callRepository)
            .function(callRepository::updateIsCameraOnById)
            .whenInvokedWith(eq(conversationId), eq(isCameraOn))
            .thenDoNothing()

        updateVideoStateUseCase(conversationId, videoState)

        verify(callRepository)
            .function(callRepository::updateIsCameraOnById)
            .with(eq(conversationId), eq(isCameraOn))
            .wasInvoked(once)

        verify(callManager)
            .suspendFunction(callManager::updateVideoState)
            .with(any(), any())
            .wasInvoked(once)
    }

    @Test
    fun givenAFlowOfEstablishedCallsThatContainsNonEstablishedCall_whenUseCaseInvoked_thenDoNotInvokeUpdateVideoState() = runTest {

        given(callRepository)
            .suspendFunction(callRepository::establishedCallsFlow)
            .whenInvoked().then {
                flowOf(listOf())
            }
        given(callRepository)
            .function(callRepository::updateIsCameraOnById)
            .whenInvokedWith(eq(conversationId), eq(isCameraOn))
            .thenDoNothing()

        updateVideoStateUseCase(conversationId, videoState)

        verify(callRepository)
            .function(callRepository::updateIsCameraOnById)
            .with(eq(conversationId), eq(isCameraOn))
            .wasInvoked(once)

        verify(callManager)
            .suspendFunction(callManager::updateVideoState)
            .with(any(), any())
            .wasNotInvoked()
    }

    companion object {
        private const val isCameraOn = true
        private val videoState = VideoState.STARTED
        private val conversationId = ConversationId("value", "domain")
    }

}
