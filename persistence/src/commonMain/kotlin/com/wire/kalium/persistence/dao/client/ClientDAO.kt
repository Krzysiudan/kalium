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

package com.wire.kalium.persistence.dao.client

import com.wire.kalium.persistence.dao.QualifiedIDEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

data class Client(
    val userId: QualifiedIDEntity,
    val id: String,
    val deviceType: DeviceTypeEntity?,
    val clientType: ClientTypeEntity?,
    val isValid: Boolean,
    val isVerified: Boolean,
    val registrationDate: Instant?,
    val label: String?,
    val model: String?,
)

data class InsertClientParam(
    val userId: QualifiedIDEntity,
    val id: String,
    val deviceType: DeviceTypeEntity?,
    val clientType: ClientTypeEntity?,
    val label: String?,
    val registrationDate: Instant?,
    val model: String?
)

enum class DeviceTypeEntity {
    Phone,
    Tablet,
    Desktop,
    LegalHold,
    Unknown;
}

enum class ClientTypeEntity {
    Permanent,
    Temporary,
    LegalHold;
}

@Suppress("TooManyFunctions")
interface ClientDAO {
    suspend fun insertClient(client: InsertClientParam)
    suspend fun insertClients(clients: List<InsertClientParam>)
    suspend fun getClientsOfUserByQualifiedIDFlow(qualifiedID: QualifiedIDEntity): Flow<List<Client>>
    suspend fun getClientsOfUserByQualifiedID(qualifiedID: QualifiedIDEntity): List<Client>
    suspend fun observeClientsByUserId(qualifiedID: QualifiedIDEntity): Flow<List<Client>>
    suspend fun getClientsOfUsersByQualifiedIDs(ids: List<QualifiedIDEntity>): Map<QualifiedIDEntity, List<Client>>
    suspend fun deleteClientsOfUserByQualifiedID(qualifiedID: QualifiedIDEntity)
    suspend fun deleteClient(userId: QualifiedIDEntity, clientId: String)
    suspend fun getClientsOfConversation(id: QualifiedIDEntity): Map<QualifiedIDEntity, List<Client>>
    suspend fun conversationRecipient(ids: QualifiedIDEntity): Map<QualifiedIDEntity, List<Client>>
    suspend fun insertClientsAndRemoveRedundant(clients: List<InsertClientParam>)
    suspend fun tryMarkInvalid(invalidClientsList: List<Pair<QualifiedIDEntity, List<String>>>)
    suspend fun updateClientVerificationStatus(userId: QualifiedIDEntity, clientId: String, verified: Boolean)
    suspend fun observeClient(userId: QualifiedIDEntity, clientId: String): Flow<Client?>
    suspend fun selectAllClients(): Map<QualifiedIDEntity, List<Client>>
}
