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

package com.wire.kalium.persistence.kmmSettings

import com.wire.kalium.persistence.client.AuthTokenStorage
import com.wire.kalium.persistence.client.TokenStorage
import com.wire.kalium.persistence.client.TokenStorageImpl
import com.wire.kalium.persistence.dbPassphrase.PassphraseStorage
import com.wire.kalium.persistence.dbPassphrase.PassphraseStorageImpl

actual class GlobalPrefProvider(
    rootPath: String,
    shouldEncryptData: Boolean = true
) {
    private val kaliumPref =
        KaliumPreferencesSettings(
            encryptedSettingsBuilder(
                SettingOptions.AppSettings(shouldEncryptData),
                EncryptedSettingsPlatformParam(rootPath)
            )
        )

    actual val authTokenStorage: AuthTokenStorage
        get() = AuthTokenStorage(kaliumPref)
    actual val passphraseStorage: PassphraseStorage
        get() = PassphraseStorageImpl(kaliumPref)
    actual val tokenStorage: TokenStorage
        get() = TokenStorageImpl(kaliumPref)
}
