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
package com.wire.kalium.logic.feature.selfdeletingMessages

import com.wire.kalium.logic.data.id.ConversationId
import kotlin.time.Duration

sealed class SelfDeletionTimer {
    /**
     * Represents a self deletion timer that is currently disabled
     */
    object Disabled : SelfDeletionTimer()

    /**
     * Represents a self deletion timer that is enabled and can be changed/updated by the user
     */
    data class Enabled(val userDuration: Duration) : SelfDeletionTimer()

    /**
     * Represents a self deletion timer that is imposed by the team or conversation settings that can't be changed by the user
     * @param enforcedDuration the team or conversation imposed timer
     */
    sealed class Enforced(val enforcedDuration: Duration) : SelfDeletionTimer() {
        data class ByTeam(val duration: Duration) : Enforced(duration)
        data class ByGroup(val duration: Duration) : Enforced(duration)
    }

    fun toDuration(): Duration = when (this) {
        is Enabled -> userDuration
        is Enforced -> enforcedDuration
        else -> Duration.ZERO
    }

    val isEnforced
        get() = this is Enforced

    val isEnforcedByTeam
        get() = this is Enforced.ByTeam

    val isEnforcedByGroup
        get() = this is Enforced.ByGroup

    val isDisabled
        get() = this is Disabled
}

data class ConversationSelfDeletionStatus(
    val conversationId: ConversationId,
    val selfDeletionTimer: SelfDeletionTimer
)

data class TeamSettingsSelfDeletionStatus(
    /**
     * This value is used to inform the user that the team settings were changed. When true, an informative dialog will be shown. Once the
     * user acknowledges or dismisses it, the value will be set again to false. When null, this means that we still don't know the real
     * value of the flag, i.e. on initial sync
     * */
    val hasFeatureChanged: Boolean?,
    /**
     * The enforced self deletion timer for the whole team. Depending on the team settings, this value will override any the conversation or
     * user settings (aka, if the team settings timer is set to [SelfDeletionTimer.Enforced] or [SelfDeletionTimer.Disabled] then the user
     * won't be able to change the timer for any conversation
     * */
    val enforcedSelfDeletionTimer: SelfDeletionTimer
)
