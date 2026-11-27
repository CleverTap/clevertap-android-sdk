package com.clevertap.android.sdk.inapp

import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoImpl
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.task.Task
import com.clevertap.android.sdk.video.VideoLibChecker
import org.json.JSONObject
import java.lang.ref.WeakReference
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class InAppNotificationInflater(
    private val storeRegistry: StoreRegistry,
    private val templatesManager: TemplatesManager,
    private val executors: CTExecutors,
    fileResourceProvider: () -> FileResourceProvider,
    private val isVideoSupported: Boolean = VideoLibChecker.haveVideoPlayerSupport
) {

    @get:WorkerThread
    private val fileResourceProvider: FileResourceProvider by lazy(fileResourceProvider)

    fun interface InAppNotificationReadyListener {
        fun onNotificationReady(notification: CTInAppNotification)
    }

    fun inflate(
        inAppJson: JSONObject,
        taskLogTag: String,
        listener: InAppNotificationReadyListener
    ) {
        val listenerWeakReference = WeakReference(listener)
        val task: Task<Unit> = executors.postAsyncSafelyTask(Constants.TAG_FEATURE_IN_APPS)
        task.execute(taskLogTag) {

            val inApp = CTInAppNotification(inAppJson, isVideoSupported)
            if (inApp.error != null) {
                notifyListener(listenerWeakReference, inApp)
                return@execute
            }
            prepareForDisplay(inApp, listenerWeakReference)
        }
    }

    private fun prepareForDisplay(
        inApp: CTInAppNotification,
        listenerWeakReference: WeakReference<InAppNotificationReadyListener>
    ) {
        if (CTInAppType.CTInAppTypeCustomCodeTemplate == inApp.inAppType) {
            processCustomTemplate(inApp)
        } else {
            processInAppMedia(inApp)
        }
        notifyListener(listenerWeakReference, inApp)
    }

    private fun processCustomTemplate(inApp: CTInAppNotification) {
        val customTemplateData = inApp.customTemplateData
        val fileUrls = customTemplateData?.getFileArgsUrls(templatesManager).orEmpty()
        val storePair = Pair(
            storeRegistry.filesStore, storeRegistry.inAppAssetsStore
        )

        for (url in fileUrls) {
            val bytes = fileResourceProvider.fetchFile(url)
            if (bytes != null && bytes.isNotEmpty()) {
                FileResourcesRepoImpl.saveUrlExpiryToStore(Pair(url, CtCacheType.FILES), storePair)
            } else {
                // download fail
                inApp.error =
                    "Error processing the custom code in-app template: file download failed."
                break
            }
        }
    }

    private fun processInAppMedia(inApp: CTInAppNotification) {
        for (media in inApp.mediaList) {
            val error = fetchMediaOrError(media)
            if (error != null) {
                inApp.error = error
                break
            }
        }
    }

    /** Fetches a gif/image [media] into the cache; returns the error to report if it is unusable, else null. */
    private fun fetchMediaOrError(media: CTInAppNotificationMedia): String? {
        return when {
            media.isGIF() -> {
                if (isGifMissing(media)) { "Error processing GIF" } else { null }
            }

            media.isImage() -> {
                val bitmap = fileResourceProvider.fetchInAppImageV1(media.mediaUrl)
                if (bitmap == null) { "Error processing image as bitmap was NULL" } else { null }
            }

            media.isVideo() || media.isAudio() -> {
                if (!isVideoSupported) { "InApp Video/Audio is not supported" } else { null }
            }

            else -> null
        }
    }

    private fun isGifMissing(media: CTInAppNotificationMedia): Boolean {
        val bytes = fileResourceProvider.fetchInAppGifV1(media.mediaUrl)
        return bytes == null || bytes.isEmpty()
    }

    private fun notifyListener(
        listenerWeakReference: WeakReference<InAppNotificationReadyListener>,
        inApp: CTInAppNotification
    ) {
        val listener = listenerWeakReference.get()
        if (listener != null) {
            executors.mainTask<Unit>().execute("InAppNotificationInflater:onNotificationReady") {
                listener.onNotificationReady(inApp)
            }
        }
    }
}
