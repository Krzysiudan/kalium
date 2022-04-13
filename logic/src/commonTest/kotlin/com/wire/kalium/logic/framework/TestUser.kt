package com.wire.kalium.logic.framework

import com.wire.kalium.logic.data.publicuser.model.OtherUser
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.persistence.dao.QualifiedIDEntity

object TestUser {
    val USER_ID = UserId("value", "domain")
    val ENTITY_ID = QualifiedIDEntity("entityUserValue", "entityDomain")

    val SELF = SelfUser(
        USER_ID,
        name = "username",
        handle = "handle",
        email = "email",
        phone = "phone",
        accentId = 0,
        team = "teamId",
        connectionStatus = ConnectionState.ACCEPTED,
        previewPicture = UserAssetId(),
        completePicture = UserAssetId()
    )

    val OTHER = OtherUser(
        USER_ID.copy(value = "otherValue"),
        name = "otherUsername",
        handle = "otherHandle",
        email = "otherEmail",
        phone = "otherPhone",
        accentId = 0,
        team = "otherTeamId",
        connectionStatus = ConnectionState.ACCEPTED,
        previewPicture = UserAssetId(),
        completePicture = UserAssetId()
    )
}
