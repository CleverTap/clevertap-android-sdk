package com.clevertap.android.sdk.inapp;

import android.graphics.Bitmap;
import android.os.Handler;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateInAppData;
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager;
import com.clevertap.android.sdk.inapp.data.CtCacheType;
import com.clevertap.android.sdk.inapp.images.FileResourceProvider;
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoImpl;
import com.clevertap.android.sdk.inapp.store.preference.FileStore;
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore;
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry;
import com.clevertap.android.sdk.task.CTExecutors;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.video.VideoLibChecker;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import kotlin.Pair;

public final class InAppNotificationCreator {

    @FunctionalInterface
    public interface InAppNotificationReadyListener {
        void onNotificationReady(CTInAppNotification notification);
    }
    private final StoreRegistry storeRegistry;
    private final FileResourceProvider fileResourceProvider;
    private final TemplatesManager templatesManager;
    private final Handler listenerHandler;
    private final CTExecutors executors;
    private final boolean isVideoSupported;

    public InAppNotificationCreator(StoreRegistry storeRegistry,
                                    FileResourceProvider fileResourceProvider,
                                    Handler listenerHandler,
                                    TemplatesManager templatesManager,
                                    CTExecutors executors,
                                    boolean isVideoSupported) {
        this.storeRegistry = storeRegistry;
        this.fileResourceProvider = fileResourceProvider;
        this.templatesManager = templatesManager;
        this.listenerHandler = listenerHandler;
        this.executors = executors;
        this.isVideoSupported = isVideoSupported;
    }

    public void createNotification(JSONObject inAppJson, String taskLogTag, InAppNotificationReadyListener listener) {
        WeakReference<InAppNotificationReadyListener> listenerWeakReference = new WeakReference<>(listener);
        Task<Void> task = executors.postAsyncSafelyTask(Constants.TAG_FEATURE_IN_APPS);
        task.execute(taskLogTag, () -> {

            final CTInAppNotification inApp = new CTInAppNotification().initWithJSON(inAppJson, isVideoSupported);
            if (inApp.getError() != null) {
                notifyListener(listenerWeakReference, inApp);
                return null;
            }
            prepareForDisplay(inApp, listenerWeakReference);
            return null;
        });
    }

    void prepareForDisplay(CTInAppNotification inApp, WeakReference<InAppNotificationReadyListener> listenerWeakReference) {

        final Pair<FileStore, InAppAssetsStore> storePair = new Pair<>(storeRegistry.getFilesStore(),
                storeRegistry.getInAppAssetsStore());

        if (CTInAppType.CTInAppTypeCustomCodeTemplate.equals(inApp.getInAppType())) {
            final CustomTemplateInAppData customTemplateData = inApp.getCustomTemplateData();
            final List<String> fileUrls;
            if (customTemplateData != null) {
                fileUrls = customTemplateData.getFileArgsUrls(templatesManager);
            } else {
                fileUrls = Collections.emptyList();
            }

            int index = 0;
            while (index < fileUrls.size()) {
                String url = fileUrls.get(index);
                byte[] bytes = fileResourceProvider.fetchFile(url);

                if (bytes != null && bytes.length > 0) {
                    FileResourcesRepoImpl.saveUrlExpiryToStore(new Pair<>(url, CtCacheType.FILES), storePair);
                } else {
                    // download fail
                    inApp.setError("Error processing the custom code in-app template: file download failed.");
                    break;
                }
                index++;
            }
        } else {
            for (CTInAppNotificationMedia media : inApp.getMediaList()) {
                if (media.isGIF()) {
                    byte[] bytes = fileResourceProvider.fetchInAppGifV1(media.getMediaUrl());
                    if (bytes == null || bytes.length == 0) {
                        inApp.setError("Error processing GIF");
                        break;
                    }
                } else if (media.isImage()) {
                    Bitmap bitmap = fileResourceProvider.fetchInAppImageV1(media.getMediaUrl());
                    if (bitmap == null) {
                        inApp.setError("Error processing image as bitmap was NULL");
                        break;
                    }
                } else if (media.isVideo() || media.isAudio()) {
                    if (!inApp.isVideoSupported()) {
                        inApp.setError("InApp Video/Audio is not supported");
                        break;
                    }
                }
            }
        }

        notifyListener(listenerWeakReference, inApp);
    }

    private void notifyListener(WeakReference<InAppNotificationReadyListener> listenerWeakReference,
                                CTInAppNotification inApp) {
        InAppNotificationReadyListener listener = listenerWeakReference.get();
        if (listener != null) {
            listenerHandler.post(() -> listener.onNotificationReady(inApp));
        }
    }
}
