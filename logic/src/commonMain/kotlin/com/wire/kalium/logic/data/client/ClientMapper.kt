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

package com.wire.kalium.logic.data.client

import com.wire.kalium.logic.configuration.ClientConfig
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.event.Event
import com.wire.kalium.logic.data.id.toDao
import com.wire.kalium.logic.data.prekey.PreKeyMapper
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.network.api.base.authenticated.client.ClientCapabilityDTO
import com.wire.kalium.network.api.base.authenticated.client.ClientResponse
import com.wire.kalium.network.api.base.authenticated.client.ClientTypeDTO
import com.wire.kalium.network.api.base.authenticated.client.DeviceTypeDTO
import com.wire.kalium.network.api.base.authenticated.client.RegisterClientRequest
import com.wire.kalium.network.api.base.authenticated.client.SimpleClientResponse
import com.wire.kalium.persistence.dao.client.ClientTypeEntity
import com.wire.kalium.persistence.dao.client.DeviceTypeEntity
import com.wire.kalium.persistence.dao.client.InsertClientParam
import kotlinx.datetime.Instant
import com.wire.kalium.network.api.base.model.UserId as UserIdDTO
import com.wire.kalium.persistence.dao.client.Client as ClientEntity

@Suppress("TooManyFunctions")
class ClientMapper(
    private val preyKeyMapper: PreKeyMapper
) {

    fun toRegisterClientRequest(
        clientConfig: ClientConfig,
        param: RegisterClientParam,
    ): RegisterClientRequest = RegisterClientRequest(
        password = param.password,
        lastKey = preyKeyMapper.toPreKeyDTO(param.lastKey),
        label = clientConfig.deviceName(),
        deviceType = toDeviceTypeDTO(clientConfig.deviceType()),
        type = param.clientType?.let { toClientTypeDTO(param.clientType) } ?: toClientTypeDTO(clientConfig.clientType()),
        capabilities = param.capabilities?.let { capabilities -> capabilities.map { toClientCapabilityDTO(it) } },
        model = clientConfig.deviceModelName(),
        preKeys = param.preKeys.map { preyKeyMapper.toPreKeyDTO(it) },
        cookieLabel = param.cookieLabel,
        secondFactorVerificationCode = param.secondFactorVerificationCode,
    )

    // TODO: mapping directly form DTO to domain object is not ideal since we lose verification information
    fun fromClientResponse(response: ClientResponse): Client = Client(
        id = ClientId(response.clientId),
        type = fromClientTypeDTO(response.type),
        registrationTime = Instant.parse(response.registrationTime),
        deviceType = fromDeviceTypeDTO(response.deviceType),
        label = response.label,
        model = response.model,
        isVerified = false,
        isValid = true
    )

    fun fromClientEntity(clientEntity: ClientEntity): Client = with(clientEntity) {
        Client(
            id = ClientId(id),
            type = clientType?.let { fromClientTypeEntity(it) },
            registrationTime = registrationDate,
            deviceType = deviceType?.let { fromDeviceTypeEntity(deviceType) },
            label = label,
            model = model,
            isVerified = isVerified,
            isValid = isValid
        )
    }

    fun fromNewClientEvent(event: Event.User.NewClient): Client = Client(
        id = event.clientId,
        type = fromClientTypeDTO(event.clientType),
        registrationTime = Instant.parse(event.registrationTime),
        deviceType = fromDeviceTypeDTO(event.deviceType),
        label = event.label,
        model = event.model,
        isVerified = false,
        isValid = true
    )

    fun toInsertClientParam(simpleClientResponse: List<SimpleClientResponse>, userIdDTO: UserIdDTO): List<InsertClientParam> =
        simpleClientResponse.map {
            with(it) {
                InsertClientParam(
                    userId = userIdDTO.toDao(),
                    id = id,
                    deviceType = toDeviceTypeEntity(deviceClass),
                    clientType = null,
                    label = null,
                    model = null,
                    registrationDate = null
                )
            }
        }

    fun toInsertClientParam(simpleClientResponse: ClientResponse, userIdDTO: UserIdDTO): InsertClientParam =
        with(simpleClientResponse) {
            InsertClientParam(
                userId = userIdDTO.toDao(),
                id = clientId,
                deviceType = toDeviceTypeEntity(deviceType),
                clientType = toClientTypeEntity(type),
                label = label,
                model = model,
                registrationDate = Instant.parse(registrationTime)
            )
        }

    fun toInsertClientParam(userId: UserId, clientId: List<ClientId>): List<InsertClientParam> =
        clientId.map {
            InsertClientParam(
                userId = userId.toDao(),
                id = it.value,
                deviceType = null,
                clientType = null,
                label = null,
                model = null,
                registrationDate = null
            )
        }

    private fun toClientTypeDTO(clientType: ClientType): ClientTypeDTO = when (clientType) {
        ClientType.Temporary -> ClientTypeDTO.Temporary
        ClientType.Permanent -> ClientTypeDTO.Permanent
        ClientType.LegalHold -> ClientTypeDTO.LegalHold
    }

    private fun fromClientTypeDTO(clientTypeDTO: ClientTypeDTO): ClientType = when (clientTypeDTO) {
        ClientTypeDTO.Temporary -> ClientType.Temporary
        ClientTypeDTO.Permanent -> ClientType.Permanent
        ClientTypeDTO.LegalHold -> ClientType.LegalHold
    }

    private fun fromClientTypeEntity(clientTypeEntity: ClientTypeEntity): ClientType = when (clientTypeEntity) {
        ClientTypeEntity.Temporary -> ClientType.Temporary
        ClientTypeEntity.Permanent -> ClientType.Permanent
        ClientTypeEntity.LegalHold -> ClientType.LegalHold
    }

    private fun toClientCapabilityDTO(clientCapability: ClientCapability): ClientCapabilityDTO = when (clientCapability) {
        ClientCapability.LegalHoldImplicitConsent -> ClientCapabilityDTO.LegalHoldImplicitConsent
    }

    private fun fromClientCapabilityDTO(clientCapabilityDTO: ClientCapabilityDTO): ClientCapability = when (clientCapabilityDTO) {
        ClientCapabilityDTO.LegalHoldImplicitConsent -> ClientCapability.LegalHoldImplicitConsent
    }

    private fun toDeviceTypeDTO(deviceType: DeviceType): DeviceTypeDTO = when (deviceType) {
        DeviceType.Phone -> DeviceTypeDTO.Phone
        DeviceType.Tablet -> DeviceTypeDTO.Tablet
        DeviceType.Desktop -> DeviceTypeDTO.Desktop
        DeviceType.LegalHold -> DeviceTypeDTO.LegalHold
        DeviceType.Unknown -> DeviceTypeDTO.Unknown
    }

    private fun fromDeviceTypeDTO(deviceTypeDTO: DeviceTypeDTO): DeviceType = when (deviceTypeDTO) {
        DeviceTypeDTO.Phone -> DeviceType.Phone
        DeviceTypeDTO.Tablet -> DeviceType.Tablet
        DeviceTypeDTO.Desktop -> DeviceType.Desktop
        DeviceTypeDTO.LegalHold -> DeviceType.LegalHold
        DeviceTypeDTO.Unknown -> DeviceType.Unknown
    }

    fun fromDeviceTypeEntity(deviceTypeEntity: DeviceTypeEntity?): DeviceType = when (deviceTypeEntity) {
        DeviceTypeEntity.Phone -> DeviceType.Phone
        DeviceTypeEntity.Tablet -> DeviceType.Tablet
        DeviceTypeEntity.Desktop -> DeviceType.Desktop
        DeviceTypeEntity.LegalHold -> DeviceType.LegalHold
        DeviceTypeEntity.Unknown, null -> DeviceType.Unknown
    }

    fun toDeviceTypeEntity(deviceTypeEntity: DeviceType): DeviceTypeEntity = when (deviceTypeEntity) {
        DeviceType.Phone -> DeviceTypeEntity.Phone
        DeviceType.Tablet -> DeviceTypeEntity.Tablet
        DeviceType.Desktop -> DeviceTypeEntity.Desktop
        DeviceType.LegalHold -> DeviceTypeEntity.LegalHold
        DeviceType.Unknown -> DeviceTypeEntity.Unknown
    }

    fun toDeviceTypeEntity(deviceTypeDTO: DeviceTypeDTO): DeviceTypeEntity = when (deviceTypeDTO) {
        DeviceTypeDTO.Phone -> DeviceTypeEntity.Phone
        DeviceTypeDTO.Tablet -> DeviceTypeEntity.Tablet
        DeviceTypeDTO.Desktop -> DeviceTypeEntity.Desktop
        DeviceTypeDTO.LegalHold -> DeviceTypeEntity.LegalHold
        DeviceTypeDTO.Unknown -> DeviceTypeEntity.Unknown
    }

    fun toClientTypeEntity(clientTypeDTO: ClientTypeDTO): ClientTypeEntity = when (clientTypeDTO) {
        ClientTypeDTO.Temporary -> ClientTypeEntity.Temporary
        ClientTypeDTO.Permanent -> ClientTypeEntity.Permanent
        ClientTypeDTO.LegalHold -> ClientTypeEntity.LegalHold
    }
}
