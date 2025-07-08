package com.clevertap.android.sdk;

import androidx.core.content.FileProvider;

/**
 * CleverTap FileProvider to avoid manifest merger conflicts
 * when apps already have their own FileProvider.
 */
public class CleverTapFileProvider extends FileProvider {
    // This class intentionally left empty
    // It simply extends FileProvider to provide a unique class name
    // for CleverTap's file sharing needs
}
