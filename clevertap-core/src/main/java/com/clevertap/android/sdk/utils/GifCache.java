package com.clevertap.android.sdk.utils;

import android.util.LruCache;

import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.inapp.CTInAppNotification;

// intended to only hold an gif byte array reference for the life of the parent CTInAppNotification, in order to facilitate parceling
public class GifCache {

    private static final int MIN_CACHE_SIZE = 1024 * 5; // 5mb minimum (in KB)

    private final static int maxMemory = (int) (Runtime.getRuntime().maxMemory()) / 1024;

    private final static int cacheSize = Math.max((maxMemory / 32), MIN_CACHE_SIZE);

    private static LruCache<String, byte[]> mMemoryCache;

    public static boolean addByteArray(String key, byte[] byteArray) {

        if (mMemoryCache == null) {
            return false;
        }

        if (getByteArray(key) == null) {
            synchronized (GifCache.class) {
                int arraySize = getByteArraySizeInKB(byteArray);
                int available = getAvailableMemory();
                Logger.v(
                        "CTInAppNotification.GifCache: gif size: " + arraySize + "KB. Available mem: " + available
                                + "KB.");
                if (arraySize > getAvailableMemory()) {
                    Logger.v("CTInAppNotification.GifCache: insufficient memory to add gif: " + key);
                    return false;
                }
                mMemoryCache.put(key, byteArray);
                Logger.v("CTInAppNotification.GifCache: added gif for key: " + key);
            }
        }
        return true;
    }

    public static byte[] getByteArray(String key) {
        synchronized (GifCache.class) {
            return mMemoryCache == null ? null : mMemoryCache.get(key);
        }
    }

    public static void init() {
        synchronized (GifCache.class) {
            if (mMemoryCache == null) {
                Logger.v("CTInAppNotification.GifCache: init with max device memory: " + maxMemory
                        + "KB and allocated cache size: " + cacheSize + "KB");
                try {
                    mMemoryCache = new LruCache<String, byte[]>(cacheSize) {
                        @Override
                        protected int sizeOf(String key, byte[] byteArray) {
                            // The cache size will be measured in kilobytes rather than
                            // number of items.
                            int size = getByteArraySizeInKB(byteArray);
                            Logger.v("CTInAppNotification.GifCache: have gif of size: " + size + "KB for key: " + key);
                            return size;
                        }
                    };
                } catch (Throwable t) {
                    Logger.v("CTInAppNotification.GifCache: unable to initialize cache: ", t.getCause());
                }
            }
        }
    }

    public static void removeByteArray(String key) {
        synchronized (GifCache.class) {
            if (mMemoryCache == null) {
                return;
            }
            mMemoryCache.remove(key);
            Logger.v("CTInAppNotification.GifCache: removed gif for key: " + key);
            cleanup();
        }
    }

    private static void cleanup() {
        synchronized (GifCache.class) {
            if (isEmpty()) {
                Logger.v("CTInAppNotification.GifCache: cache is empty, removing it");
                mMemoryCache = null;
            }
        }
    }

    private static int getAvailableMemory() {
        synchronized (GifCache.class) {
            return mMemoryCache == null ? 0 : cacheSize - mMemoryCache.size();
        }
    }

    private static int getByteArraySizeInKB(byte[] byteArray) {
        return byteArray.length / 1024;
    }

    private static boolean isEmpty() {
        synchronized (GifCache.class) {
            return mMemoryCache.size() <= 0;
        }
    }
}
