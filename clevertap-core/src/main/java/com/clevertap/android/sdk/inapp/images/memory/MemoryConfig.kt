package com.clevertap.android.sdk.inapp.images.memory

import java.io.File
import androidx.annotation.RestrictTo
/**
 * Configuration dataclass for memory management.
 *
 * This class holds parameters that define how memory is allocated and managed for caching purposes.
 *
 * @property minInMemorySizeKB The minimum size of the in-memory cache in kilobytes.
 * @property optimistic An optimistic estimate of memory availability, potentially used for additional caching.
 * @property maxDiskSizeKB The maximum size of the file allowed on disk in kilobytes.
 * @property diskDirectory The directory on disk where cached files will be stored.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class MemoryConfig(
    val minInMemorySizeKB: Long,
    val optimistic: Long,
    val maxDiskSizeKB: Long,
    val diskDirectory: File
)
