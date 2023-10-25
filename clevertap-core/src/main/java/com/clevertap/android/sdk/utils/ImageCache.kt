package com.clevertap.android.sdk.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object ImageCache {
    private const val MIN_CACHE_SIZE = 1024 * 20 // 20mb minimum (in KB)
    private val maxMemory = Runtime.getRuntime().maxMemory().toInt() / 1024
    private val cacheSize = Math.max(maxMemory / 32, MIN_CACHE_SIZE)
    private const val MAX_BITMAP_SIZE = 10000000 // 10 MB
    private const val DIRECTORY_NAME = "CleverTap.Images."
    private const val FILE_PREFIX = "CT_IMAGE_"
    private var memoryCache: LruCache<String, Bitmap?>? = null
    private var imageFileDirectory: File? = null
    private var messageDigest: MessageDigest? = null

    // only adds to mem cache, use getForFetchBitmap for disk cache support
    @JvmStatic
    fun addBitmap(key: String, bitmap: Bitmap?): Boolean {
        if (memoryCache == null) {
            return false
        }
        if (getBitmapFromMemCache(key) == null) {
            synchronized(ImageCache::class.java) {
                val imageSize = getImageSizeInKB(bitmap)
                val available = availableMemory
                Logger.v(
                    "CleverTap.ImageCache: image size: " + imageSize + "KB. Available mem: " + available + "KB."
                )
                if (imageSize > availableMemory) {
                    Logger.v("CleverTap.ImageCache: insufficient memory to add image: $key")
                    return false
                }
                memoryCache!!.put(key, bitmap)
                Logger.v("CleverTap.ImageCache: added image for key: $key")
            }
        }
        return true
    }

    // only checks mem cache and will not load a missing image, use getForFetchBitmap for loading and disk cache support
    @JvmStatic
    fun getBitmap(key: String?): Bitmap? {
        synchronized(ImageCache::class.java) {
            return if (key != null) {
                if (memoryCache == null) null else memoryCache!![key]
            } else {
                null
            }
        }
    }

    // potentially blocking, will always persist to the file system.  for mem cache only use addBitmap + getBitmap
    fun getOrFetchBitmap(url: String): Bitmap? {
        var bitmap = getBitmap(url)
        if (bitmap == null) {
            val imageFile = getOrFetchAndWriteImageFile(url)
            if (imageFile != null) {
                bitmap = decodeImageFromFile(imageFile)
                addBitmap(url, bitmap)
            } else {
                return null
            }
        }
        return bitmap
    }

    @JvmStatic
    fun init() {
        synchronized(ImageCache::class.java) {
            if (memoryCache == null) {
                Logger.v(
                    "CleverTap.ImageCache: init with max device memory: " + maxMemory
                            + "KB and allocated cache size: " + cacheSize + "KB"
                )
                try {
                    memoryCache = object : LruCache<String, Bitmap?>(cacheSize) {
                        override fun sizeOf(key: String, bitmap: Bitmap?): Int {
                            // The cache size will be measured in kilobytes rather than
                            // number of items.
                            val size = getImageSizeInKB(bitmap)
                            Logger.v("CleverTap.ImageCache: have image of size: " + size + "KB for key: " + key)
                            return size
                        }
                    }
                } catch (t: Throwable) {
                    Logger.v("CleverTap.ImageCache: unable to initialize cache: ", t.cause)
                }
            }
        }
    }

    fun initWithPersistence(context: Context) {
        synchronized(ImageCache::class.java) {
            if (imageFileDirectory == null) {
                imageFileDirectory = context.getDir(DIRECTORY_NAME, Context.MODE_PRIVATE)
            }
            if (messageDigest == null) {
                try {
                    messageDigest = MessageDigest.getInstance("SHA256")
                } catch (e: NoSuchAlgorithmException) {
                    Logger.d(
                        "CleverTap.ImageCache: image file system caching unavailable as SHA1 hash function not available on platform"
                    )
                }
            }
        }
        init()
    }

    @JvmStatic
    fun removeBitmap(key: String, isPersisted: Boolean) {
        synchronized(ImageCache::class.java) {
            if (isPersisted) {
                removeFromFileSystem(key)
            }
            if (memoryCache == null) {
                return
            }
            memoryCache!!.remove(key)
            Logger.v("CleverTap.ImageCache: removed image for key: $key")
            cleanup()
        }
    }

    private fun cleanup() {
        synchronized(ImageCache::class.java) {
            if (isEmpty) {
                Logger.v("CTInAppNotification.ImageCache: cache is empty, removing it")
                memoryCache = null
            }
        }
    }

    private fun decodeImageFromFile(file: File): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = false
        BitmapFactory.decodeFile(file.absolutePath, options)
        val imageSize = options.outHeight.toFloat() * options.outWidth * 4
        val imageSizeKb = imageSize / 1024
        if (imageSizeKb > availableMemory) {
            Logger.v("CleverTap.ImageCache: image too large to decode")
            return null
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        if (bitmap == null) {
            file.delete()
        }
        return bitmap
    }

    private val availableMemory: Int
        get() {
            synchronized(ImageCache::class.java) { return if (memoryCache == null) 0 else cacheSize - memoryCache!!.size() }
        }

    private fun getBitmapFromMemCache(key: String?): Bitmap? {
        return if (key != null) {
            if (memoryCache == null) {
                null
            } else {
                memoryCache!![key]
            }
        } else {
            null
        }
    }

    private fun getFile(url: String): File? {
        if (messageDigest == null) {
            return null
        }
        val hashed = messageDigest!!.digest(url.toByteArray())
        val safeName =
            FILE_PREFIX + Base64.encodeToString(hashed, Base64.URL_SAFE or Base64.NO_WRAP)
        return File(imageFileDirectory, safeName)
    }

    private fun getImageSizeInKB(bitmap: Bitmap?): Int {
        return bitmap!!.byteCount / 1024
    }

    // will do a blocking network fetch if file does not already exist
    private fun getOrFetchAndWriteImageFile(url: String): File? {
        val file = getFile(url)
        val bytes: ByteArray?
        if (file == null || !file.exists()) {
            bytes = Utils.getByteArrayFromImageURL(url) // blocking network operation
            if (bytes != null) {
                if (file != null && bytes.size < MAX_BITMAP_SIZE) {
                    var out: OutputStream? = null
                    try {
                        out = FileOutputStream(file)
                        out.write(bytes)
                    } catch (e: FileNotFoundException) {
                        Logger.v("CleverTap.ImageCache: error writing image file", e)
                        return null
                    } catch (e: IOException) {
                        Logger.v("CleverTap.ImageCache: error writing image file", e)
                        return null
                    } finally {
                        if (out != null) {
                            try {
                                out.close()
                            } catch (e: IOException) {
                                Logger.v("CleverTap.ImageCache: error closing image output file", e)
                            }
                        }
                    }
                }
            }
        }
        return file
    }

    private val isEmpty: Boolean
        get() {
            synchronized(ImageCache::class.java) { return memoryCache!!.size() <= 0 }
        }

    private fun removeFromFileSystem(url: String) {
        val file = getFile(url)
        if (file != null && file.exists()) {
            file.delete()
        }
    }
}