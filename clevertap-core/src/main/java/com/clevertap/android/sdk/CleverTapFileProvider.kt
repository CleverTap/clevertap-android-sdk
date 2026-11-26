package com.clevertap.android.sdk

import androidx.core.content.FileProvider
import androidx.annotation.RestrictTo

/**
 * CleverTap FileProvider to avoid manifest merger conflicts
 * when apps already have their own FileProvider.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CleverTapFileProvider : FileProvider()
