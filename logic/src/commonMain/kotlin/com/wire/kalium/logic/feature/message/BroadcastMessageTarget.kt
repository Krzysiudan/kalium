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
package com.wire.kalium.logic.feature.message

sealed class BroadcastMessageTarget(open val limit: Int) {

    /** Broadcast the message to all users in contact list (teammates and others)
     * @param limit message will be broadcasted only to the first [limit] Users (teammates are prioritized)
     */
    data class AllUsers(override val limit: Int) : BroadcastMessageTarget(limit)

    /** Broadcast the message only to the teammates
     * @param limit message will be broadcasted only to the first [limit] teammates.
     */
    data class OnlyTeam(override val limit: Int) : BroadcastMessageTarget(limit)
}
