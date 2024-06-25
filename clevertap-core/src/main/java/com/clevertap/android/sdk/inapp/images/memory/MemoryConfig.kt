package com.clevertap.android.sdk.inapp.images.memory

import java.io.File

data class MemoryConfig(
    val minInMemorySizeKB: Long,
    val optimistic: Long,
    val maxDiskSizeKB: Long,
    val diskDirectory: File
)