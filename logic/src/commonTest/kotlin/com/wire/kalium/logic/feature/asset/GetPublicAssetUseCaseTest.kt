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

package com.wire.kalium.logic.feature.asset

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.asset.AssetRepository
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.functional.Either
import io.mockative.Mock
import io.mockative.classOf
import io.mockative.eq
import io.mockative.given
import io.mockative.mock
import io.mockative.once
import io.mockative.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class GetPublicAssetUseCaseTest {

    @Mock
    private val assetRepository = mock(classOf<AssetRepository>())

    private lateinit var getPublicAsset: GetAvatarAssetUseCase

    @BeforeTest
    fun setUp() {
        getPublicAsset = GetAvatarAssetUseCaseImpl(assetRepository)
    }

    @Test
    fun givenACallToGetAPublicAsset_whenEverythingGoesWell_thenShouldReturnsASuccessResultWithData() = runTest {
        val assetKey = UserAssetId("value1", "domain")
        val expectedPath = "expected_encrypted_path".toPath()

        given(assetRepository)
            .suspendFunction(assetRepository::downloadPublicAsset)
            .whenInvokedWith(eq(assetKey.value), eq(assetKey.domain))
            .thenReturn(Either.Right(expectedPath))

        val publicAsset = getPublicAsset(assetKey)

        assertEquals(PublicAssetResult.Success::class, publicAsset::class)
        assertEquals(expectedPath, (publicAsset as PublicAssetResult.Success).assetPath)

        verify(assetRepository)
            .suspendFunction(assetRepository::downloadPublicAsset)
            .with(eq(assetKey.value), eq(assetKey.domain))
            .wasInvoked(exactly = once)
    }

    @Test
    fun givenACallToGetAPublicAsset_whenEverythingThereIsAnError_thenShouldReturnsAFailureResult() = runTest {
        val assetKey = UserAssetId("value1", "domain")

        given(assetRepository)
            .suspendFunction(assetRepository::downloadPublicAsset)
            .whenInvokedWith(eq(assetKey.value), eq(assetKey.domain))
            .thenReturn(Either.Left(CoreFailure.Unknown(Throwable("an error"))))

        val publicAsset = getPublicAsset(assetKey)

        assertEquals(PublicAssetResult.Failure::class, publicAsset::class)
        assertEquals(CoreFailure.Unknown::class, (publicAsset as PublicAssetResult.Failure).coreFailure::class)

        verify(assetRepository)
            .suspendFunction(assetRepository::downloadPublicAsset)
            .with(eq(assetKey.value), eq(assetKey.domain))
            .wasInvoked(exactly = once)
    }
}
