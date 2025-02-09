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

package com.wire.kalium.logic.network

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.feature.CurrentClientIdProvider
import com.wire.kalium.logic.feature.session.UpgradeCurrentSessionUseCase
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.functional.flatMapLeft

class ApiMigrationV3(
    private val currentClientIdProvider: CurrentClientIdProvider,
    private val upgradeCurrentSession: UpgradeCurrentSessionUseCase,
) : ApiMigration {
    override suspend operator fun invoke(): Either<CoreFailure, Unit> =
        currentClientIdProvider().flatMap {
            upgradeCurrentSession(it)
        }.flatMapLeft {
            if (it is StorageFailure.DataNotFound) {
                Either.Right(Unit)
            } else {
                Either.Left(it)
            }
        }
}
