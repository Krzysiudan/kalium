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
package com.wire.kalium.logic.data.service

import app.cash.turbine.test
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.toModel
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.util.shouldFail
import com.wire.kalium.logic.util.shouldSucceed
import com.wire.kalium.persistence.dao.BotIdEntity
import com.wire.kalium.persistence.dao.QualifiedIDEntity
import com.wire.kalium.persistence.dao.ServiceDAO
import com.wire.kalium.persistence.dao.ServiceEntity
import io.mockative.Mock
import io.mockative.any
import io.mockative.eq
import io.mockative.given
import io.mockative.mock
import io.mockative.once
import io.mockative.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceRepositoryTest {

    @Test
    fun givenSuccess_whenReadingServiceInfoById_thenSuccessIsPropagated() = runTest {
        val expected = serviceDetails

        val (arrangement, serviceRepository) = Arrangement()
            .withServiceByIdSuccess(serviceIdEntity, serviceEntity)
            .arrange()

        serviceRepository.getServiceById(serviceId).also {
            it.shouldSucceed {
                assertEquals(serviceDetails, it)
            }
        }

        verify(arrangement.serviceDAO)
            .suspendFunction(arrangement.serviceDAO::byId)
            .with(any())
            .wasInvoked(exactly = once)
    }

    @Test
    fun givenError_whenReadingServiceInfoById_thenErrorIsPropagated() = runTest {
        val expected = StorageFailure.Generic(IllegalStateException())

        val (arrangement, serviceRepository) = Arrangement()
            .withServiceByIdFailure(serviceIdEntity, expected.rootCause)
            .arrange()

        serviceRepository.getServiceById(serviceId).also {
            it.shouldFail {
                assertEquals(expected, it)
            }
        }

        verify(arrangement.serviceDAO)
            .suspendFunction(arrangement.serviceDAO::byId)
            .with(any())
            .wasInvoked(exactly = once)
    }

    @Test
    fun givenSuccess_whenObservingIfServiceIsMember_thenSuccessIsPropagated() = runTest {
        val expected: List<QualifiedIDEntity?> = listOf(null, QualifiedIDEntity("id", "domain"), null)

        val (arrangement, serviceRepository) = Arrangement()
            .withObserveIsMemberSuccess(expected.asFlow())
            .arrange()

        serviceRepository.observeIsServiceMember(serviceId, ConversationId("id", "domain")).test {
            expected.forEach { expectedEmit ->
                val currentValue = awaitItem()
                currentValue.shouldSucceed {
                    assertEquals(expectedEmit?.toModel(), it)
                }
            }
            awaitComplete()
        }

        verify(arrangement.serviceDAO)
            .suspendFunction(arrangement.serviceDAO::observeIsServiceMember)
            .with(any(), any())
            .wasInvoked(exactly = once)
    }

    @Test
    fun givenError_whenObservingIfServiceIsMember_thenErrorIsPropagated() = runTest {
        val expected = StorageFailure.Generic(IllegalStateException())

        val (arrangement, serviceRepository) = Arrangement()
            .withObserveIsMemberFailure(expected.rootCause)
            .arrange()

        serviceRepository.observeIsServiceMember(serviceId, ConversationId("id", "domain")).first().also {
            it.shouldFail {
                assertEquals(expected, it)
            }
        }

        verify(arrangement.serviceDAO)
            .suspendFunction(arrangement.serviceDAO::observeIsServiceMember)
            .with(any(), any())
            .wasInvoked(exactly = once)
    }

    @Test
    fun givenSuccess_whenObservingAllServices_thenSuccessIsPropagated() = runTest {
        val expectedEntity = listOf(
            serviceEntity.copy(id = BotIdEntity("id1", "domain1")),
            serviceEntity.copy(id = BotIdEntity("id2", "domain2"))
        )

        val expected = listOf(
            serviceDetails.copy(id = ServiceId("id1", "domain1")),
            serviceDetails.copy(id = ServiceId("id2", "domain2"))
        )


        val (arrangement, serviceRepository) = Arrangement()
            .withGetAllServicesSuccess(flowOf(expectedEntity))
            .arrange()

        serviceRepository.observeAllServices().test {
            val currentValue = awaitItem()
            assertIs<Either.Right<List<ServiceDetails>>>(currentValue)
            assertEquals(expected, currentValue.value)

            awaitComplete()
        }

        verify(arrangement.serviceDAO)
            .suspendFunction(arrangement.serviceDAO::getAllServices)
            .wasInvoked(exactly = once)
    }

    @Test
    fun givenError_whenObservingAllServices_thenErrorIsPropagated() = runTest {
        val expected = StorageFailure.Generic(IllegalStateException())

        val (arrangement, serviceRepository) = Arrangement()
            .withGetAllServicesFailure(expected.rootCause)
            .arrange()

        serviceRepository.observeAllServices().test {
            val currentValue = awaitItem()
            assertIs<Either.Left<StorageFailure>>(currentValue)
            assertEquals(expected, currentValue.value)

            awaitComplete()
        }

        verify(arrangement.serviceDAO)
            .suspendFunction(arrangement.serviceDAO::getAllServices)
            .wasInvoked(exactly = once)
    }

    @Test
    fun givenSuccess_whenSearchingServicesByName_thenSearchResultIsPropagated() = runTest {
        val expectedEntity = listOf(
            serviceEntity.copy(id = BotIdEntity("id1", "domain1")),
            serviceEntity.copy(id = BotIdEntity("id2", "domain2"))
        )

        val expected = listOf(
            serviceDetails.copy(id = ServiceId("id1", "domain1")),
            serviceDetails.copy(id = ServiceId("id2", "domain2"))
        )

        val (arrangement, serviceRepository) = Arrangement()
            .withSearchServicesByNameSuccess(flowOf(expectedEntity))
            .arrange()

        serviceRepository.searchServicesByName("name").test {
            val currentValue = awaitItem()
            assertIs<Either.Right<List<ServiceDetails>>>(currentValue)
            assertEquals(expected, currentValue.value)

            awaitComplete()
        }

        verify(arrangement.serviceDAO)
            .suspendFunction(arrangement.serviceDAO::searchServicesByName)
            .with(any())
            .wasInvoked(exactly = once)
    }

    @Test
    fun givenError_whenSearchingServicesByName_thenErrorIsPropagated() = runTest {
        val expected = StorageFailure.Generic(IllegalStateException())

        val (arrangement, serviceRepository) = Arrangement()
            .withSearchServicesByNameFailure(expected.rootCause)
            .arrange()

        serviceRepository.searchServicesByName("name").test {
            val currentValue = awaitItem()
            assertIs<Either.Left<StorageFailure>>(currentValue)
            assertEquals(expected, currentValue.value)

            awaitComplete()
        }

        verify(arrangement.serviceDAO)
            .suspendFunction(arrangement.serviceDAO::searchServicesByName)
            .with(any())
            .wasInvoked(exactly = once)
    }

    private companion object {
        val serviceIdEntity = BotIdEntity("serviceId", "providerId")
        val serviceId = ServiceId("serviceId", "providerId")

        val serviceEntity = ServiceEntity(
            id = serviceIdEntity,
            name = "name",
            description = "description",
            completeAssetId = null,
            previewAssetId = null,
            enabled = true,
            summary = "summary",
            tags = emptyList()
        )

        val serviceDetails = ServiceDetails(
            id = serviceId,
            name = "name",
            description = "description",
            completeAssetId = null,
            previewAssetId = null,
            enabled = true,
            summary = "summary",
            tags = emptyList()
        )
    }


    private class Arrangement {
        @Mock
        val serviceDAO: ServiceDAO = mock(ServiceDAO::class)

        private val serviceRepository: ServiceRepository = ServiceDataSource(serviceDAO)

        fun withServiceByIdSuccess(serviceId: BotIdEntity, result: ServiceEntity?) = apply {
            given(serviceDAO)
                .suspendFunction(serviceDAO::byId)
                .whenInvokedWith(eq(serviceId))
                .then { result }
        }

        fun withServiceByIdFailure(serviceId: BotIdEntity, error: Throwable) = apply {
            given(serviceDAO)
                .suspendFunction(serviceDAO::byId)
                .whenInvokedWith(eq(serviceId))
                .thenThrow(error)
        }

        fun withObserveIsMemberSuccess(result: Flow<QualifiedIDEntity?>) = apply {
            given(serviceDAO)
                .suspendFunction(serviceDAO::observeIsServiceMember)
                .whenInvokedWith(any(), any())
                .thenReturn(result)
        }

        fun withObserveIsMemberFailure(error: Throwable) = apply {
            given(serviceDAO)
                .suspendFunction(serviceDAO::observeIsServiceMember)
                .whenInvokedWith(any(), any())
                .thenThrow(error)
        }

        fun withGetAllServicesSuccess(result: Flow<List<ServiceEntity>>) = apply {
            given(serviceDAO)
                .suspendFunction(serviceDAO::getAllServices)
                .whenInvoked()
                .thenReturn(result)
        }

        fun withGetAllServicesFailure(error: Throwable) = apply {
            given(serviceDAO)
                .suspendFunction(serviceDAO::getAllServices)
                .whenInvoked()
                .thenThrow(error)
        }

        fun withSearchServicesByNameSuccess(result: Flow<List<ServiceEntity>>) = apply {
            given(serviceDAO)
                .suspendFunction(serviceDAO::searchServicesByName)
                .whenInvokedWith(any())
                .thenReturn(result)
        }

        fun withSearchServicesByNameFailure(error: Throwable) = apply {
            given(serviceDAO)
                .suspendFunction(serviceDAO::searchServicesByName)
                .whenInvokedWith(any())
                .thenThrow(error)
        }

        fun arrange() = this to serviceRepository

    }
}
