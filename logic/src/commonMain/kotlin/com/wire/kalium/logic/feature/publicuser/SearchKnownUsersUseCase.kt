package com.wire.kalium.logic.feature.publicuser

import com.wire.kalium.logic.data.publicuser.SearchUserRepository
import com.wire.kalium.logic.data.publicuser.model.UserSearchResult
import kotlinx.coroutines.flow.Flow

interface SearchKnownUsersUseCase {
    suspend operator fun invoke(searchQuery: String): Flow<UserSearchResult>
}

internal class SearchKnownUsersUseCaseImpl(
    private val searchUserRepository: SearchUserRepository
) : SearchKnownUsersUseCase {

    override suspend operator fun invoke(searchQuery: String): Flow<UserSearchResult> =
        searchUserRepository.searchKnownUsers(searchQuery)

}
