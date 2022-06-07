package com.wire.kalium.logic.feature.call

import app.cash.turbine.test
import com.wire.kalium.logic.data.call.CallRepository
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsUseCase
import com.wire.kalium.logic.sync.SyncManager
import io.mockative.Mock
import io.mockative.classOf
import io.mockative.given
import io.mockative.mock
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAllCallsUseCaseTest {

    @Mock
    private val callRepository = mock(classOf<CallRepository>())

    @Mock
    private val syncManager = mock(classOf<SyncManager>())

    private lateinit var getAllCallsUseCase: GetAllCallsUseCase

    @BeforeTest
    fun setUp() {
        getAllCallsUseCase = GetAllCallsUseCase(
            callRepository = callRepository,
            syncManager = syncManager
        )
    }

    @Test
    fun givenCallsFlowEmitsANewValue_whenUseCaseIsCollected_thenAssertThatTheUseCaseIsEmittingTheRightCalls() = runTest {
        val calls1 = listOf(call1, call2)
        val calls2 = listOf(call2)

        val callsFlow = flowOf(calls1, calls2)
        given(syncManager).invocation { startSyncIfIdle() }
            .thenReturn(Unit)
        given(callRepository).invocation { callsFlow() }
            .then { callsFlow }

        val result = getAllCallsUseCase()

        result.test {
            assertEquals(calls1, awaitItem())
            assertEquals(calls2, awaitItem())
            awaitComplete()
        }
    }

    companion object {
        private val call1 = Call(
            conversationId = ConversationId("first", "domain"),
            status = CallStatus.STARTED,
            callerId = "caller-id",
            isMuted = true,
            isCameraOn = false
        )
        private val call2 = Call(
            conversationId = ConversationId("second", "domain"),
            status = CallStatus.INCOMING,
            callerId = "caller-id",
            isMuted = true,
            isCameraOn = false
        )
    }

}
