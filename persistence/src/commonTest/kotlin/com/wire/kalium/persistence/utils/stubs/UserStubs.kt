package com.wire.kalium.persistence.utils.stubs

import com.wire.kalium.persistence.dao.QualifiedIDEntity
import com.wire.kalium.persistence.dao.UserEntity

fun newUserEntity(id: String = "test") =
    UserEntity(
        id = QualifiedIDEntity(id, "wire.com"),
        name = "user$id",
        handle = "handle$id",
        email = "email$id",
        phone = "phone$id",
        accentId = 1,
        team = "team",
        UserEntity.ConnectionState.ACCEPTED,
        null, null
    )

fun newUserEntity(qualifiedID: QualifiedIDEntity, id: String = "test") =
    UserEntity(
        id = qualifiedID,
        name = "user$id",
        handle = "handle$id",
        email = "email$id",
        phone = "phone$id",
        accentId = 1,
        team = "team",
        UserEntity.ConnectionState.ACCEPTED,
        null, null
    )

fun newUserEntity(
    id: String = "test",
    name: String = "testName",
    handle: String = "testHandle",
    email: String = "testEmail@wire.com",
    phone: String = "testPhone",
    accentId: Int = 1,
    team: String = "testTeam",
    connectionStatus: UserEntity.ConnectionState = UserEntity.ConnectionState.ACCEPTED,
    previewAssetId: String = "previewAssetId",
    completeAssetId: String = "completeAssetId",
): UserEntity {
    return UserEntity(
        id = QualifiedIDEntity(id, "wire.com"),
        name = name,
        handle = handle,
        email = email,
        phone = phone,
        accentId = accentId,
        team = team,
        connectionStatus = connectionStatus,
        previewAssetId = previewAssetId,
        completeAssetId = completeAssetId
    )
}
