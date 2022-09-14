package com.wire.kalium.persistence.dao

import app.cash.turbine.test
import com.wire.kalium.persistence.BaseDatabaseTest
import com.wire.kalium.persistence.db.UserDatabaseProvider
import com.wire.kalium.persistence.utils.stubs.newUserEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserDAOTest : BaseDatabaseTest() {

    private val user1 = newUserEntity(id = "1")
    private val user2 = newUserEntity(id = "2")
    private val user3 = newUserEntity(id = "3")

    lateinit var db: UserDatabaseProvider

    @BeforeTest
    fun setUp() {
        deleteDatabase()
        db = createDatabase()
    }

    @Test
    fun givenUser_ThenUserCanBeInserted() = runTest {
        db.userDAO.insertUser(user1)
        val result = db.userDAO.getUserByQualifiedID(user1.id).first()
        assertEquals(result, user1)
    }

    @Test
    fun givenListOfUsers_ThenMultipleUsersCanBeInsertedAtOnce() = runTest {
        db.userDAO.upsertUsers(listOf(user1, user2, user3))
        val result1 = db.userDAO.getUserByQualifiedID(user1.id).first()
        val result2 = db.userDAO.getUserByQualifiedID(user2.id).first()
        val result3 = db.userDAO.getUserByQualifiedID(user3.id).first()
        assertEquals(result1, user1)
        assertEquals(result2, user2)
        assertEquals(result3, user3)
    }

    @Test
    fun givenExistingUser_ThenUserCanBeDeleted() = runTest {
        db.userDAO.insertUser(user1)
        db.userDAO.deleteUserByQualifiedID(user1.id)
        val result = db.userDAO.getUserByQualifiedID(user1.id).first()
        assertNull(result)
    }

    @Test
    fun givenExistingUser_ThenUserCanBeUpdated() = runTest {
        db.userDAO.insertUser(user1)
        val updatedUser1 = UserEntity(
            user1.id,
            "John Doe",
            "johndoe",
            "email1",
            "phone1",
            1,
            "team",
            ConnectionEntity.State.ACCEPTED,
            UserAssetIdEntity("asset1", "domain"),
            UserAssetIdEntity("asset1", "domain"),
            UserAvailabilityStatusEntity.NONE,
            UserTypeEntity.STANDARD,
            botService = null,
            false
        )
        db.userDAO.updateSelfUser(updatedUser1)
        val result = db.userDAO.getUserByQualifiedID(user1.id).first()
        assertEquals(result, updatedUser1)
    }

    @Test
    fun givenListOfUsers_ThenUserCanBeQueriedByName() = runTest {
        db.userDAO.insertUser(user1)
        val updatedUser1 = UserEntity(
            user1.id,
            "John Doe",
            "johndoe",
            "email1",
            "phone1",
            1,
            "team",
            ConnectionEntity.State.ACCEPTED,
            UserAssetIdEntity("asset1", "domain"),
            UserAssetIdEntity("asset2", "domain"),
            UserAvailabilityStatusEntity.NONE,
            UserTypeEntity.STANDARD,
            botService = null,
            false
        )

        val result = db.userDAO.getUserByQualifiedID(user1.id)
        assertEquals(user1, result.first())

        db.userDAO.updateSelfUser(updatedUser1)
        assertEquals(updatedUser1, result.first())
    }

    @Test
    fun givenRetrievedUser_ThenUpdatesArePropagatedThroughFlow() = runTest {
        db.userDAO.insertUser(user1)
        val updatedUser1 = UserEntity(
            user1.id,
            "John Doe",
            "johndoe",
            "email1",
            "phone1",
            1,
            "team",
            ConnectionEntity.State.ACCEPTED,
            null,
            null,
            UserAvailabilityStatusEntity.NONE,
            UserTypeEntity.STANDARD,
            botService = null,
            false
        )

        val result = db.userDAO.getUserByQualifiedID(user1.id)
        assertEquals(user1, result.first())

        db.userDAO.updateSelfUser(updatedUser1)
        assertEquals(updatedUser1, result.first())
    }

    @Test
    fun givenAExistingUsers_WhenQueriedUserByUserEmail_ThenResultsIsEqualToThatUser() = runTest {
        // given
        val user1 = USER_ENTITY_1
        val user2 = USER_ENTITY_2.copy(email = "uniqueEmailForUser2")
        val user3 = USER_ENTITY_3
        db.userDAO.upsertUsers(listOf(user1, user2, user3))
        // when
        launch(UnconfinedTestDispatcher(testScheduler)) {
            db.userDAO.getUserByNameOrHandleOrEmailAndConnectionStates(
                user2.email!!,
                listOf(ConnectionEntity.State.ACCEPTED)
            )
                .test {
                    // then
                    val searchResult = awaitItem()
                    assertEquals(searchResult, listOf(user2))
                    cancelAndIgnoreRemainingEvents()
                }
        }

    }

    @Test
    fun givenAExistingUsers_WhenQueriedUserByName_ThenResultsIsEqualToThatUser(): TestResult {
        return runTest {
            // given
            val user1 = USER_ENTITY_1
            val user2 = USER_ENTITY_3
            val user3 = USER_ENTITY_3.copy(handle = "uniqueHandlForUser3")
            db.userDAO.upsertUsers(listOf(user1, user2, user3))
            // when
            launch(UnconfinedTestDispatcher(testScheduler)) {
                db.userDAO.getUserByNameOrHandleOrEmailAndConnectionStates(
                    user3.handle!!,
                    listOf(ConnectionEntity.State.ACCEPTED)
                )
                    .test {
                        // then
                        val searchResult = awaitItem()
                        assertEquals(searchResult, listOf(user3))
                        cancelAndIgnoreRemainingEvents()
                    }
            }
        }
    }

    @Test
    fun givenAExistingUsers_WhenQueriedUserByHandle_ThenResultsIsEqualToThatUser() = runTest {
        // given
        val user1 = USER_ENTITY_1.copy(name = "uniqueNameFor User1")
        val user2 = USER_ENTITY_2
        val user3 = USER_ENTITY_3
        db.userDAO.upsertUsers(listOf(user1, user2, user3))
        // when
        launch(UnconfinedTestDispatcher(testScheduler)) {
            db.userDAO.getUserByNameOrHandleOrEmailAndConnectionStates(user1.name!!, listOf(ConnectionEntity.State.ACCEPTED))
                .test {
                    val searchResult = awaitItem()
                    assertEquals(searchResult, listOf(user1))
                    cancelAndIgnoreRemainingEvents()
                }
        }
    }

    @Test
    fun givenAExistingUsersWithCommonEmailPrefix_WhenQueriedWithThatEmailPrefix_ThenResultIsEqualToTheUsersWithCommonEmailPrefix() =
        runTest {
            // given
            val commonEmailPrefix = "commonEmail"

            val commonEmailUsers = listOf(
                USER_ENTITY_1.copy(email = commonEmailPrefix + "u1@example.org"),
                USER_ENTITY_2.copy(email = commonEmailPrefix + "u2@example.org"),
                USER_ENTITY_3.copy(email = commonEmailPrefix + "u3@example.org")
            )
            val notCommonEmailUsers = listOf(
                UserEntity(
                    id = QualifiedIDEntity("4", "wire.com"),
                    name = "testName4",
                    handle = "testHandle4",
                    email = "someDifferentEmail1@wire.com",
                    phone = "testPhone4",
                    accentId = 4,
                    team = "testTeam4",
                    ConnectionEntity.State.ACCEPTED,
                    null,
                    null,
                    UserAvailabilityStatusEntity.NONE,
                    UserTypeEntity.STANDARD,
                    botService = null,
                    false
                ),
                UserEntity(
                    id = QualifiedIDEntity("5", "wire.com"),
                    name = "testName5",
                    handle = "testHandle5",
                    email = "someDifferentEmail2@wire.com",
                    phone = "testPhone5",
                    accentId = 5,
                    team = "testTeam5",
                    ConnectionEntity.State.ACCEPTED,
                    null,
                    null,
                    UserAvailabilityStatusEntity.NONE,
                    UserTypeEntity.STANDARD,
                    botService = null,
                    deleted = false
                )
            )
            val mockUsers = commonEmailUsers + notCommonEmailUsers

            db.userDAO.upsertUsers(mockUsers)
            // when
            launch(UnconfinedTestDispatcher(testScheduler)) {
                db.userDAO.getUserByNameOrHandleOrEmailAndConnectionStates(commonEmailPrefix, listOf(ConnectionEntity.State.ACCEPTED))
                    .test {
                        // then
                        val searchResult = awaitItem()
                        assertEquals(searchResult, commonEmailUsers)
                        cancelAndIgnoreRemainingEvents()
                    }
            }
        }

    // when entering
    @Test
    fun givenAExistingUsers_WhenQueriedWithNonExistingEmail_ThenReturnNoResults() = runTest {
        // given
        val mockUsers = listOf(USER_ENTITY_1, USER_ENTITY_2, USER_ENTITY_3)
        db.userDAO.upsertUsers(mockUsers)

        val nonExistingEmailQuery = "doesnotexist@wire.com"
        // when
        launch(UnconfinedTestDispatcher(testScheduler)) {
            db.userDAO.getUserByNameOrHandleOrEmailAndConnectionStates(
                nonExistingEmailQuery,
                listOf(ConnectionEntity.State.ACCEPTED)
            )
                .test {
                    // then
                    val searchResult = awaitItem()
                    assertTrue { searchResult.isEmpty() }
                    cancelAndIgnoreRemainingEvents()
                }
        }
    }

    @Test
    fun givenAExistingUsers_whenQueriedWithCommonEmailPrefix_ThenResultsUsersEmailContainsThatPrefix() = runTest {
        // given
        val commonEmailPrefix = "commonEmail"

        val mockUsers = listOf(USER_ENTITY_1, USER_ENTITY_2, USER_ENTITY_3)
        db.userDAO.upsertUsers(mockUsers)
        // when
        launch(UnconfinedTestDispatcher(testScheduler)) {
            db.userDAO.getUserByNameOrHandleOrEmailAndConnectionStates(
                commonEmailPrefix,
                listOf(ConnectionEntity.State.ACCEPTED)
            )
                .test {
                    // then
                    val searchResult = awaitItem()
                    searchResult.forEach { userEntity ->
                        assertContains(userEntity.email!!, commonEmailPrefix)
                    }
                    cancelAndIgnoreRemainingEvents()
                }
        }
    }

    @Test
    fun givenAExistingUsers_whenQueriedWithCommonHandlePrefix_ThenResultsUsersHandleContainsThatPrefix() = runTest {
        // given
        val commonHandlePrefix = "commonHandle"

        val mockUsers = listOf(USER_ENTITY_1, USER_ENTITY_2, USER_ENTITY_3)
        db.userDAO.upsertUsers(mockUsers)
        // when
        launch(UnconfinedTestDispatcher(testScheduler)) {
            db.userDAO.getUserByNameOrHandleOrEmailAndConnectionStates(
                commonHandlePrefix,
                listOf(ConnectionEntity.State.ACCEPTED)
            )
                .test {
                    // then
                    val searchResult = awaitItem()
                    searchResult.forEach { userEntity ->
                        assertContains(userEntity.handle!!, commonHandlePrefix)
                    }
                    cancelAndIgnoreRemainingEvents()
                }
        }
    }

    @Test
    fun givenAExistingUsers_whenQueriedWithCommonNamePrefix_ThenResultsUsersNameContainsThatPrefix() = runTest {
        // given
        val commonNamePrefix = "commonName"

        val mockUsers = listOf(USER_ENTITY_1, USER_ENTITY_2, USER_ENTITY_3)
        db.userDAO.upsertUsers(mockUsers)
        // when
        launch(UnconfinedTestDispatcher(testScheduler)) {
            db.userDAO.getUserByNameOrHandleOrEmailAndConnectionStates(
                commonNamePrefix,
                listOf(ConnectionEntity.State.ACCEPTED)
            )
                .test {
                    // then
                    val searchResult = awaitItem()
                    searchResult.forEach { userEntity ->
                        assertContains(userEntity.name!!, commonNamePrefix)
                    }
                    cancelAndIgnoreRemainingEvents()
                }
        }
    }

    @Test
    fun givenAExistingUsers_whenQueriedWithCommonPrefixForNameHandleAndEmail_ThenResultsUsersNameHandleAndEmailContainsThatPrefix() =
        runTest {
            // given
            val commonPrefix = "common"

            val mockUsers = listOf(
                USER_ENTITY_1.copy(name = commonPrefix + "u1"),
                USER_ENTITY_2.copy(handle = commonPrefix + "u2"),
                USER_ENTITY_3.copy(email = commonPrefix + "u3")
            )
            db.userDAO.upsertUsers(mockUsers)
            // when
            launch(UnconfinedTestDispatcher(testScheduler)) {
                db.userDAO.getUserByNameOrHandleOrEmailAndConnectionStates(commonPrefix, listOf(ConnectionEntity.State.ACCEPTED))
                    .test {
                        // then
                        val searchResult = awaitItem()
                        assertEquals(mockUsers, searchResult)
                        cancelAndIgnoreRemainingEvents()
                    }
            }
        }

    @Test
    fun givenAExistingUsers_whenQueried_ThenResultsUsersAreConnected() = runTest {
        // given
        val commonPrefix = "common"

        val mockUsers = listOf(
            USER_ENTITY_1.copy(name = commonPrefix + "u1"),
            USER_ENTITY_2.copy(handle = commonPrefix + "u2"),
            USER_ENTITY_3.copy(email = commonPrefix + "u3")
        )

        db.userDAO.upsertUsers(mockUsers)
        // when
        launch(UnconfinedTestDispatcher(testScheduler)) {
            db.userDAO.getUserByNameOrHandleOrEmailAndConnectionStates(
                commonPrefix,
                listOf(ConnectionEntity.State.ACCEPTED)
            )
                .test {
                    // then
                    val searchResult = awaitItem()
                    searchResult.forEach { userEntity ->
                        assertEquals(ConnectionEntity.State.ACCEPTED, userEntity.connectionStatus)
                    }
                    cancelAndIgnoreRemainingEvents()
                }
        }
    }

    @Test
    fun givenAConnectedExistingUsersAndNonConnected_whenQueried_ThenResultsUsersAreConnected() = runTest {
        // given
        val commonPrefix = "common"

        val mockUsers = listOf(
            USER_ENTITY_1.copy(name = commonPrefix + "u1"),
            USER_ENTITY_2.copy(handle = commonPrefix + "u2"),
            USER_ENTITY_3.copy(email = commonPrefix + "u3", connectionStatus = ConnectionEntity.State.NOT_CONNECTED),
            USER_ENTITY_4.copy(email = commonPrefix + "u4", connectionStatus = ConnectionEntity.State.NOT_CONNECTED)
        )

        db.userDAO.upsertUsers(mockUsers)
        // when
        launch(UnconfinedTestDispatcher(testScheduler)) {
            db.userDAO.getUserByNameOrHandleOrEmailAndConnectionStates(commonPrefix, listOf(ConnectionEntity.State.ACCEPTED))
                .test {
                    // then
                    val searchResult = awaitItem()
                    assertTrue(searchResult.size == 2)
                    searchResult.forEach { userEntity ->
                        assertEquals(ConnectionEntity.State.ACCEPTED, userEntity.connectionStatus)
                    }
                    cancelAndIgnoreRemainingEvents()
                }
        }
    }

    @Test
    fun givenAConnectedExistingUserAndNonConnected_whenQueried_ThenResultIsTheConnectedUser() = runTest {
        // given
        val commonPrefix = "common"

        val expectedResult = listOf(USER_ENTITY_1.copy(name = commonPrefix + "u1"))

        val mockUsers = listOf(
            USER_ENTITY_1.copy(name = commonPrefix + "u1"),
            USER_ENTITY_2.copy(handle = commonPrefix + "u2", connectionStatus = ConnectionEntity.State.NOT_CONNECTED),
            USER_ENTITY_3.copy(name = commonPrefix + "u3", connectionStatus = ConnectionEntity.State.NOT_CONNECTED),
            USER_ENTITY_4.copy(email = commonPrefix + "u4", connectionStatus = ConnectionEntity.State.NOT_CONNECTED)
        )

        db.userDAO.upsertUsers(mockUsers)
        // when
        launch(UnconfinedTestDispatcher(testScheduler)) {
            db.userDAO.getUserByNameOrHandleOrEmailAndConnectionStates(
                commonPrefix,
                listOf(ConnectionEntity.State.ACCEPTED)
            )
                .test {
                    // then
                    val searchResult = awaitItem()
                    assertEquals(expectedResult, searchResult)
                    cancelAndIgnoreRemainingEvents()
                }
        }
    }

    @Test
    fun givenTheListOfUser_whenQueriedByHandle_ThenResultContainsOnlyTheUserHavingTheHandleAndAreConnected() = runTest {
        val expectedResult = listOf(
            USER_ENTITY_1.copy(handle = "@someHandle"),
            USER_ENTITY_4.copy(handle = "@someHandle1")
        )

        val mockUsers = listOf(
            USER_ENTITY_1.copy(handle = "@someHandle"),
            USER_ENTITY_2.copy(connectionStatus = ConnectionEntity.State.NOT_CONNECTED),
            USER_ENTITY_3.copy(connectionStatus = ConnectionEntity.State.NOT_CONNECTED),
            USER_ENTITY_4.copy(handle = "@someHandle1")
        )

        db.userDAO.upsertUsers(mockUsers)

        // when
        launch(UnconfinedTestDispatcher(testScheduler)) {
            db.userDAO.getUserByHandleAndConnectionStates(
                "some",
                listOf(ConnectionEntity.State.ACCEPTED)
            )
                .test {
                    // then
                    val searchResult = awaitItem()
                    assertEquals(expectedResult, searchResult)
                    cancelAndIgnoreRemainingEvents()
                }
        }
    }

    @Test
    fun givenAExistingUsers_whenUpdatingTheirValues_ThenResultsIsEqualToThatUserButWithFieldsModified() = runTest {
        // given
        val newNameA = "new user naming a"
        val newNameB = "new user naming b"
        db.userDAO.upsertUsers(listOf(user1, user3))
        // when
        val updatedUser1 = user1.copy(name = newNameA)
        val updatedUser3 = user3.copy(name = newNameB)
        db.userDAO.upsertUsers(listOf(updatedUser1, updatedUser3))
        // then
        val updated1 = db.userDAO.getUserByQualifiedID(updatedUser1.id)
        val updated3 = db.userDAO.getUserByQualifiedID(updatedUser3.id)
        assertEquals(newNameA, updated1.first()?.name)
        assertEquals(newNameB, updated3.first()?.name)
    }

    @Test
    fun givenAExistingUsers_whenUpdatingTheirValuesAndRecordNotExists_ThenResultsOneUpdatedAnotherInserted() = runTest {
        // given
        val newNameA = "new user naming a"
        db.userDAO.insertUser(user1)
        // when
        val updatedUser1 = user1.copy(name = newNameA)
        db.userDAO.upsertUsers(listOf(updatedUser1, user2))
        // then
        val updated1 = db.userDAO.getUserByQualifiedID(updatedUser1.id)
        val inserted2 = db.userDAO.getUserByQualifiedID(user2.id)
        assertEquals(newNameA, updated1.first()?.name)
        assertNotNull(inserted2)
    }

    @Test
    fun givenAExistingUsers_whenUpsertingTeamMembers_ThenResultsOneUpdatedAnotherInserted() = runTest {
        // given
        val newTeamId = "new user team id"
        db.userDAO.insertUser(user1)
        // when
        val updatedUser1 = user1.copy(team = newTeamId)
        db.userDAO.upsertTeamMembersTypes(listOf(updatedUser1, user2))
        // then
        val updated1 = db.userDAO.getUserByQualifiedID(updatedUser1.id)
        val inserted2 = db.userDAO.getUserByQualifiedID(user2.id)
        assertEquals(newTeamId, updated1.first()?.team)
        assertNotNull(inserted2)
    }

    @Test
    fun givenATeamMember_whenUpsertingTeamMember_ThenUserTypeShouldStayTheSame() = runTest {
        // given
        val externalMember = user1.copy(userType = UserTypeEntity.EXTERNAL)
        db.userDAO.upsertTeamMembersTypes(listOf(externalMember))
        // when
        db.userDAO.upsertTeamMembers(listOf(user1))
        // then
        val updated1 = db.userDAO.getUserByQualifiedID(user1.id)
        assertEquals(UserTypeEntity.EXTERNAL, updated1.first()?.userType)
    }

    @Test
    fun givenAExistingUsers_whenUpsertingUsers_ThenResultsOneUpdatedAnotherInsertedWithNoConnectionStatusOverride() = runTest {
        // given
        val newTeamId = "new team id"
        db.userDAO.insertUser(user1.copy(connectionStatus = ConnectionEntity.State.ACCEPTED))
        // when
        val updatedUser1 = user1.copy(team = newTeamId)
        db.userDAO.upsertUsers(listOf(updatedUser1, user2))
        // then
        val updated1 = db.userDAO.getUserByQualifiedID(updatedUser1.id)
        val inserted2 = db.userDAO.getUserByQualifiedID(user2.id)
        assertEquals(newTeamId, updated1.first()?.team)
        assertEquals(ConnectionEntity.State.ACCEPTED, updated1.first()?.connectionStatus)
        assertNotNull(inserted2)
    }

    @Test
    fun givenListOfUsers_WhenGettingListOfUsers_ThenMatchingUsersAreReturned() = runTest {
        val users = listOf(user1, user2)
        val requestedIds = (users + user3).map { it.id }
        db.userDAO.upsertUsers(users)
        val result = db.userDAO.getUsersByQualifiedIDList(requestedIds)
        assertEquals(result, users)
        assertTrue(!result.contains(user3))
    }

    private companion object {
        val USER_ENTITY_1 = newUserEntity(QualifiedIDEntity("1", "wire.com"))
        val USER_ENTITY_2 = newUserEntity(QualifiedIDEntity("2", "wire.com"))
        val USER_ENTITY_3 = newUserEntity(QualifiedIDEntity("3", "wire.com"))
        val USER_ENTITY_4 = newUserEntity(QualifiedIDEntity("4", "wire.com"))
    }

}
