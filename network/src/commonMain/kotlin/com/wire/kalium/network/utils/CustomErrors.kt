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

package com.wire.kalium.network.utils

import com.wire.kalium.network.api.base.model.ErrorResponse
import com.wire.kalium.network.exceptions.KaliumException
import com.wire.kalium.network.exceptions.NetworkErrorLabel

object CustomErrors {
    val MISSING_REFRESH_TOKEN =
        NetworkResponse.Error(
            KaliumException.ServerError(
                ErrorResponse(
                    500,
                    "no cookie was found",
                    NetworkErrorLabel.KaliumCustom.MISSING_REFRESH_TOKEN
                )
            )
        )

    val MISSING_NONCE =
        NetworkResponse.Error(
            KaliumException.ServerError(
                ErrorResponse(
                    500,
                    "no nonce found",
                    NetworkErrorLabel.KaliumCustom.MISSING_NONCE
                )
            )
        )
}
