package com.clevertap.android.sdk.inapp.images.cleanup

import com.clevertap.android.sdk.inapp.images.FileResourceProvider

/**
 * A strategy for cleaning up file assets.
 *
 * Implementations of this interface define the logic for clearing files based on provided URLs.
 */
internal interface FileCleanupStrategy{

    val fileResourceProvider: () -> FileResourceProvider

    /**
     * Clears file assets associated with the given URLs.
     *
     * @param urls A list of URLs representing the file assets to be cleared.
     * @param successBlock A function to be executed for each URL that is successfully cleared.
     */
    fun clearFileAssets(urls: List<String>, successBlock: (url: String) -> Unit)
    /**
     * Stops or terminates the ongoing file cleanup process.
     */
    fun stop()
}