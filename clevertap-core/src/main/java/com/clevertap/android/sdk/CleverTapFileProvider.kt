package com.clevertap.android.sdk

import androidx.core.content.FileProvider

/**
 * CleverTap FileProvider to avoid manifest merger conflicts
 * when apps already have their own FileProvider.
 */
class CleverTapFileProvider : FileProvider()
