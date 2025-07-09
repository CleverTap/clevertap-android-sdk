package com.clevertap.android.sdk.inapp.images

import android.content.Context
import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.data.CtCacheType.FILES
import com.clevertap.android.sdk.inapp.data.CtCacheType.GIF
import com.clevertap.android.sdk.inapp.data.CtCacheType.IMAGE
import com.clevertap.android.sdk.inapp.images.memory.FileMemoryAccessObject
import com.clevertap.android.sdk.inapp.images.memory.InAppGifMemoryAccessObjectV1
import com.clevertap.android.sdk.inapp.images.memory.InAppImageMemoryAccessObjectV1
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType
import com.clevertap.android.sdk.inapp.images.memory.MemoryAccessObject
import com.clevertap.android.sdk.inapp.images.memory.MemoryCreator
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToBitmap
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToByteArray
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToFile
import com.clevertap.android.sdk.inapp.images.repo.TAG_FILE_DOWNLOAD
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.utils.CTCaches
import java.io.File

internal class FileResourceProvider(
    images: File,
    gifs: File,
    allFileTypesDir: File,
    private val logger: ILogger? = null,
    private val inAppRemoteSource: FileFetchApiContract = FileFetchApi(),
    ctCaches: CTCaches = CTCaches.instance(
        inAppImageMemoryV1 = MemoryCreator.createInAppImageMemoryV1(images, logger),
        inAppGifMemoryV1 = MemoryCreator.createInAppGifMemoryV1(gifs, logger),
        fileMemory = MemoryCreator.createFileMemoryV2(allFileTypesDir, logger)
    ),
    private val imageMAO: InAppImageMemoryAccessObjectV1 = InAppImageMemoryAccessObjectV1(
        ctCaches,
        logger
    ),
    private val gifMAO: InAppGifMemoryAccessObjectV1 = InAppGifMemoryAccessObjectV1(
        ctCaches,
        logger
    ),
    private val fileMAO: FileMemoryAccessObject = FileMemoryAccessObject(ctCaches, logger),
    private val deepLogging: Boolean = false
) {
    private val mapOfMAO: Map<CtCacheType, List<MemoryAccessObject<*>>> =
        mapOf<CtCacheType, List<MemoryAccessObject<*>>>(
            IMAGE to listOf(imageMAO, fileMAO, gifMAO),
            GIF to listOf(gifMAO, fileMAO, imageMAO),
            FILES to listOf(fileMAO, imageMAO, gifMAO)
        )
    constructor(
        context: Context,
        logger: ILogger? = null
    ) : this(
        images = context.getDir(IMAGE_DIRECTORY_NAME, Context.MODE_PRIVATE),
        gifs = context.getDir(GIF_DIRECTORY_NAME, Context.MODE_PRIVATE),
        allFileTypesDir = context.getDir(ALL_FILE_TYPES_DIRECTORY_NAME, Context.MODE_PRIVATE),
        logger = logger
    )

    companion object {
        private const val IMAGE_DIRECTORY_NAME = "CleverTap.Images."
        private const val GIF_DIRECTORY_NAME = "CleverTap.Gif."
        private const val ALL_FILE_TYPES_DIRECTORY_NAME = "CleverTap.Files."

        @Volatile
        private var instance: FileResourceProvider? = null

        @JvmStatic
        fun getInstance(context: Context, logger: ILogger? = null): FileResourceProvider {
            return instance ?: synchronized(this) {
                instance ?: FileResourceProvider(
                    images = context.getDir(IMAGE_DIRECTORY_NAME, Context.MODE_PRIVATE),
                    gifs = context.getDir(GIF_DIRECTORY_NAME, Context.MODE_PRIVATE),
                    allFileTypesDir = context.getDir(
                        ALL_FILE_TYPES_DIRECTORY_NAME,
                        Context.MODE_PRIVATE
                    ),
                    logger = logger
                ).also { instance = it }
            }
        }
    }

    private fun <T> saveData(
        cacheKey: String,
        data: Pair<T, ByteArray>,
        mao: MemoryAccessObject<T>
    ) {
        val savedFile = mao.saveDiskMemory(cacheKey, data.second)
        mao.saveInMemory(cacheKey, Pair(data.first, savedFile))
    }

    fun isFileCached(url: String): Boolean {
        return (mapOfMAO[FILES]?.run {
            firstNotNullOfOrNull {// Try in memory
                it.fetchInMemory(url)
            } ?: firstNotNullOfOrNull {/* Try disk */
                it.fetchDiskMemory(url)
            }
        }) != null
    }

    fun cachedInAppImageV1(cacheKey: String?): Bitmap? =
        fetchCachedData(Pair(cacheKey, IMAGE), ToBitmap)

    fun cachedInAppGifV1(cacheKey: String?): ByteArray? =
        fetchCachedData(Pair(cacheKey, GIF), ToByteArray)

    fun cachedFileInBytes(cacheKey: String?): ByteArray? =
        fetchCachedData(Pair(cacheKey, FILES), ToByteArray)

    fun cachedFilePath(cacheKey: String?): String? = cachedFileInstance(cacheKey)?.absolutePath
    fun cachedFileInstance(cacheKey: String?): File? =
        fetchCachedData(Pair(cacheKey, FILES), ToFile)

    /**
     * Function that would fetch and cache bitmap image into Memory and File cache and return it.
     * If image is found in cache, the cached image is returned.
     */
    fun fetchInAppImageV1(url: String): Bitmap? {
        return fetchData(
            urlMeta = Pair(url, IMAGE),
            mao = imageMAO,
            cachedDataFetcherBlock = ::cachedInAppImageV1
        ) { downloadedBitmap ->
            when (downloadedBitmap.status) {
                DownloadedBitmap.Status.SUCCESS -> {
                    // force unwrapping cause successful
                    Pair(downloadedBitmap.bitmap!!, downloadedBitmap.bytes!!)
                }
                else -> {
                    null
                }
            }
        }
    }

    fun fetchInAppGifV1(url: String): ByteArray? {
        return fetchData(
            urlMeta = Pair(url, GIF),
            mao = gifMAO,
            cachedDataFetcherBlock = ::cachedInAppGifV1,
            dataToSaveBlock = ::downloadedBytesFromApi
        )
    }

    fun fetchFile(url: String): ByteArray? {
        return fetchData(
            urlMeta = Pair(url, FILES),
            mao = fileMAO,
            cachedDataFetcherBlock = ::cachedFileInBytes,
            dataToSaveBlock = ::downloadedBytesFromApi
        )
    }

    private fun downloadedBytesFromApi(downloadedBitmap: DownloadedBitmap): Pair<ByteArray, ByteArray>? {
        return when (downloadedBitmap.status) {
            DownloadedBitmap.Status.SUCCESS -> {
                // force unwrap cause successful
                Pair(downloadedBitmap.bytes!!, downloadedBitmap.bytes)
            }
            else -> {
                null
            }
        }
    }

    fun deleteData(cacheKey: String) {
        mapOfMAO[IMAGE]?.forEach { mao ->
            val cacheType = when (mao) {
                is InAppImageMemoryAccessObjectV1 -> IMAGE
                is InAppGifMemoryAccessObjectV1 -> GIF
                is FileMemoryAccessObject -> FILES
                else -> ""
            }
            val pair = mao.removeInMemory(cacheKey)
            if (pair != null) {
                log("$cacheKey was present in $cacheType in-memory cache is successfully removed")
            }

            val b = mao.removeDiskMemory(cacheKey)
            if (b) {
                log("$cacheKey was present in $cacheType disk-memory cache is successfully removed")
            }
        }
    }


    private fun <T> fetchCachedData(cacheKeyAndType: Pair<String?,CtCacheType>, transformationType: MemoryDataTransformationType<T>): T? {
        val cacheKey = cacheKeyAndType.first
        val cacheType = cacheKeyAndType.second
        log("${cacheType.name} data for key $cacheKey requested")

        if (cacheKey == null) {
            log("${cacheType.name} data for null key requested")
            return null
        }

        return mapOfMAO[cacheType]?.run {
            firstNotNullOfOrNull {// Try in memory
                it.fetchInMemoryAndTransform(cacheKey, transformationType)
            } ?: firstNotNullOfOrNull {/* Try disk */
                it.fetchDiskMemoryAndTransform(cacheKey, transformationType)
            }
        }
    }

    private fun <T> fetchData(
        urlMeta: Pair<String, CtCacheType>,
        mao: MemoryAccessObject<T>,
        cachedDataFetcherBlock: (String) -> T?,
        dataToSaveBlock: (DownloadedBitmap) -> Pair<T, ByteArray>?
    ): T? {
        val cachedData = cachedDataFetcherBlock(urlMeta.first)

        if (cachedData != null) {
            log("Returning requested ${urlMeta.first} ${urlMeta.second.name} from cache")
            return cachedData
        }

        val downloadedData: DownloadedBitmap = inAppRemoteSource.makeApiCallForFile(urlMeta)
        return when (downloadedData.status) {
            DownloadedBitmap.Status.SUCCESS -> {
                val dataToSave: Pair<T, ByteArray> = dataToSaveBlock(downloadedData)!! // can force unwrap since success case
                saveData(
                    cacheKey = urlMeta.first,
                    data = dataToSave,
                    mao = mao
                )
                log("Returning requested ${urlMeta.first} ${urlMeta.second.name} with network, saved in cache")
                dataToSave.first
            }
            else -> {
                log("There was a problem fetching data for ${urlMeta.second.name}, status: ${downloadedData.status}")
                null
            }
        }
    }

    private fun log(message: String) {
        if (deepLogging) {
            logger?.verbose(TAG_FILE_DOWNLOAD, message)
        }
    }
}