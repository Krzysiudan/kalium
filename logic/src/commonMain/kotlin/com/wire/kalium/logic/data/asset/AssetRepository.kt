package com.wire.kalium.logic.data.asset

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.di.MapperProvider
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.suspending
import com.wire.kalium.logic.wrapApiRequest
import com.wire.kalium.logic.wrapStorageRequest
import com.wire.kalium.network.api.asset.AssetApi
import com.wire.kalium.persistence.dao.asset.AssetDAO
import kotlinx.coroutines.flow.firstOrNull

interface AssetRepository {
    /**
     * Method used to upload and persist to local memory a public asset
     * @param mimeType type of the asset to be uploaded
     * @param rawAssetData unencrypted data to be uploaded
     * @return [Either] a [CoreFailure] if anything went wrong, or the [UploadedAssetId] of the asset if successful
     */
    suspend fun uploadAndPersistPublicAsset(mimeType: AssetType, rawAssetData: ByteArray): Either<CoreFailure, UploadedAssetId>

    /**
     * Method used to upload and persist to local memory a private asset
     * @param mimeType type of the asset to be uploaded
     * @param encryptedAssetData encrypted data to be uploaded
     * @return [Either] a [CoreFailure] if anything went wrong, or the [UploadedAssetId] of the asset if successful
     */
    suspend fun uploadAndPersistPrivateAsset(mimeType: AssetType, encryptedAssetData: ByteArray): Either<CoreFailure, UploadedAssetId>

    /**
     * Method used to download and persist to local memory a public asset
     * @param assetKey the asset identifier
     * @return [Either] a [CoreFailure] if anything went wrong, or the asset as a decoded ByteArray of data
     */
    suspend fun downloadPublicAsset(assetKey: String): Either<CoreFailure, ByteArray>

    /**
     * Method used to download and persist to local memory a private asset
     * @param assetKey the asset identifier
     * @param assetToken the asset token used to provide an extra layer of asset/user authentication
     * @return [Either] a [CoreFailure] if anything went wrong, or the asset as an encoded ByteArray of data
     */
    suspend fun downloadPrivateAsset(assetKey: String, assetToken: String?): Either<CoreFailure, ByteArray>

    /**
     * Method used to download the list of avatar pictures of the current logged in user
     * @param assetIdList list of the assets' id that wants to be downloaded
     * @return [Either] a [CoreFailure] if anything went wrong, or Unit if operation was successful
     */
    suspend fun downloadUsersPictureAssets(assetIdList: List<UserAssetId?>): Either<CoreFailure, Unit>
}

internal class AssetDataSource(
    private val assetApi: AssetApi,
    private val assetDao: AssetDAO,
    private val assetMapper: AssetMapper = MapperProvider.assetMapper()
) : AssetRepository {

    override suspend fun uploadAndPersistPublicAsset(mimeType: AssetType, rawAssetData: ByteArray): Either<CoreFailure, UploadedAssetId> {
        val uploadAssetData = UploadAssetData(rawAssetData, mimeType, true, RetentionType.ETERNAL)
        return uploadAndPersistAsset(uploadAssetData)
    }

    override suspend fun uploadAndPersistPrivateAsset(
        mimeType: AssetType,
        encryptedAssetData: ByteArray
    ): Either<CoreFailure, UploadedAssetId> {
        val uploadAssetData = UploadAssetData(encryptedAssetData, mimeType, false, RetentionType.PERSISTENT)
        return uploadAndPersistAsset(uploadAssetData)
    }

    private suspend fun uploadAndPersistAsset(uploadAssetData: UploadAssetData): Either<CoreFailure, UploadedAssetId> = suspending {
        assetMapper.toMetadataApiModel(uploadAssetData).let { metaData ->
            wrapApiRequest {
                // we should also consider for avatar images, the compression for preview vs complete picture
                assetApi.uploadAsset(metaData, uploadAssetData.data)
            }
        }.flatMap { assetResponse ->
            assetMapper.fromUploadedAssetToDaoModel(uploadAssetData, assetResponse).let { assetEntity ->
                wrapStorageRequest { assetDao.insertAsset(assetEntity) }
            }.map { assetMapper.fromApiUploadResponseToDomainModel(assetResponse) }
        }
    }

    override suspend fun downloadPublicAsset(assetKey: String): Either<CoreFailure, ByteArray> = suspending {
        downloadAsset(assetKey = assetKey, assetToken = null)
    }

    override suspend fun downloadPrivateAsset(assetKey: String, assetToken: String?): Either<CoreFailure, ByteArray> = suspending {
        downloadAsset(assetKey = assetKey, assetToken = assetToken)
    }

    private suspend fun downloadAsset(assetKey: String, assetToken: String?): Either<CoreFailure, ByteArray> = suspending {
        wrapStorageRequest { assetDao.getAssetByKey(assetKey).firstOrNull() }.coFold({
            wrapApiRequest {
                // Backend sends asset messages with empty asset tokens
                assetApi.downloadAsset(assetKey, assetToken?.ifEmpty { null })
            }.flatMap { assetData ->
                wrapStorageRequest { assetDao.insertAsset(assetMapper.fromUserAssetToDaoModel(assetKey, assetData)) }
                    .map { assetData }
            }
        }, { Either.Right(it.rawData) })
    }

    override suspend fun downloadUsersPictureAssets(assetIdList: List<UserAssetId?>): Either<CoreFailure, Unit> = suspending {
        assetIdList.filterNotNull().forEach {
            downloadPublicAsset(it)
        }
        return@suspending Either.Right(Unit)
    }
}
