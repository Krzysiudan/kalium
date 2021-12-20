package com.wire.kalium.network.api.prekey

import com.wire.kalium.network.utils.NetworkResponse

interface PreKeyApi {
    /**
     * @param users a map of domain to (map of user IDs to client IDs)
     * @return a prekey for each one. You can't request information for more users than maximum conversation size.
     */
    suspend fun getUsersPreKey(users: DomainToUserIdToClientsMap): NetworkResponse<DomainToUserIdToClientsToPreykeyMap>

    suspend fun getClientAvailablePrekeys(clientId: String): NetworkResponse<List<Int>>

}

typealias DomainToUserIdToClientsToPreykeyMap = Map<String, Map<String, Map<String, PreKey>>>
typealias DomainToUserIdToClientsMap = Map<String, Map<String, List<String>>>

