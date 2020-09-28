package com.clevertap.android.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.LruCache;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ImageCache {

    private static final int MIN_CACHE_SIZE = 1024 * 20; // 20mb minimum (in KB)

    private final static int maxMemory = (int) (Runtime.getRuntime().maxMemory()) / 1024;

    private final static int cacheSize = Math.max((maxMemory / 32), MIN_CACHE_SIZE);

    private static final int MAX_BITMAP_SIZE = 10000000; // 10 MB

    private static final String DIRECTORY_NAME = "CleverTap.Images.";

    private static final String FILE_PREFIX = "CT_IMAGE_";

    private static LruCache<String, Bitmap> mMemoryCache;

    private static File imageFileDirectory;

    private static MessageDigest messageDigest;

    @SuppressWarnings("WeakerAccess")
    // only adds to mem cache, use getForFetchBitmap for disk cache support
    public static boolean addBitmap(String key, Bitmap bitmap) {
        if (mMemoryCache == null) {
            return false;
        }
        if (getBitmapFromMemCache(key) == null) {
            synchronized (ImageCache.class) {
                int imageSize = getImageSizeInKB(bitmap);
                int available = getAvailableMemory();
                Logger.v(
                        "CleverTap.ImageCache: image size: " + imageSize + "KB. Available mem: " + available + "KB.");
                if (imageSize > getAvailableMemory()) {
                    Logger.v("CleverTap.ImageCache: insufficient memory to add image: " + key);
                    return false;
                }
                mMemoryCache.put(key, bitmap);
                Logger.v("CleverTap.ImageCache: added image for key: " + key);
            }
        }
        return true;
    }

    // only checks mem cache and will not load a missing image, use getForFetchBitmap for loading and disk cache support
    @SuppressWarnings("WeakerAccess")
    public static Bitmap getBitmap(String key) {
        synchronized (ImageCache.class) {
            if (key != null) {
                return mMemoryCache == null ? null : mMemoryCache.get(key);
            } else {
                return null;
            }
        }
    }

    // potentially blocking, will always persist to the file system.  for mem cache only use addBitmap + getBitmap
    public static Bitmap getOrFetchBitmap(String url) {
        Bitmap bitmap = getBitmap(url);
        if (bitmap == null) {
            final File imageFile = getOrFetchAndWriteImageFile(url);
            if (imageFile != null) {
                bitmap = decodeImageFromFile(imageFile);
                addBitmap(url, bitmap);
            } else {
                return null;
            }
        }
        return bitmap;
    }

    public static void init() {
        synchronized (ImageCache.class) {
            if (mMemoryCache == null) {
                Logger.v("CleverTap.ImageCache: init with max device memory: " + maxMemory
                        + "KB and allocated cache size: " + cacheSize + "KB");
                try {
                    mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
                        @Override
                        protected int sizeOf(String key, Bitmap bitmap) {
                            // The cache size will be measured in kilobytes rather than
                            // number of items.
                            int size = getImageSizeInKB(bitmap);
                            Logger.v("CleverTap.ImageCache: have image of size: " + size + "KB for key: " + key);
                            return size;
                        }
                    };
                } catch (Throwable t) {
                    Logger.v("CleverTap.ImageCache: unable to initialize cache: ", t.getCause());
                }
            }
        }
    }

    public static void initWithPersistence(Context context) {
        synchronized (ImageCache.class) {
            if (imageFileDirectory == null) {
                imageFileDirectory = context.getDir(DIRECTORY_NAME, Context.MODE_PRIVATE);
            }
            if (messageDigest == null) {
                try {
                    messageDigest = MessageDigest.getInstance("SHA1");
                } catch (NoSuchAlgorithmException e) {
                    Logger.d(
                            "CleverTap.ImageCache: image file system caching unavailable as SHA1 hash function not available on platform");
                }
            }
        }
        init();
    }

    public static void removeBitmap(String key, boolean isPersisted) {
        synchronized (ImageCache.class) {
            if (isPersisted) {
                removeFromFileSystem(key);
            }
            if (mMemoryCache == null) {
                return;
            }
            mMemoryCache.remove(key);
            Logger.v("CleverTap.ImageCache: removed image for key: " + key);
            cleanup();
        }
    }

    private static void cleanup() {
        synchronized (ImageCache.class) {
            if (isEmpty()) {
                Logger.v("CTInAppNotification.ImageCache: cache is empty, removing it");
                mMemoryCache = null;
            }
        }
    }

    private static Bitmap decodeImageFromFile(File file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        float imageSize = (float) options.outHeight * options.outWidth * 4;
        float imageSizeKb = imageSize / 1024;
        if (imageSizeKb > getAvailableMemory()) {
            Logger.v("CleverTap.ImageCache: image too large to decode");
            return null;
        }
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (bitmap == null) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
        return bitmap;
    }

    private static int getAvailableMemory() {
        synchronized (ImageCache.class) {
            return mMemoryCache == null ? 0 : cacheSize - mMemoryCache.size();
        }
    }

    private static Bitmap getBitmapFromMemCache(String key) {
        if (key != null) {
            return mMemoryCache == null ? null : mMemoryCache.get(key);
        }
        return null;
    }

    private static File getFile(String url) {
        if (messageDigest == null) {
            return null;
        }
        final byte[] hashed = messageDigest.digest(url.getBytes());
        final String safeName = FILE_PREFIX + Base64.encodeToString(hashed, Base64.URL_SAFE | Base64.NO_WRAP);
        return new File(imageFileDirectory, safeName);
    }

    private static int getImageSizeInKB(Bitmap bitmap) {
        return bitmap.getByteCount() / 1024;
    }

    // will do a blocking network fetch if file does not already exist
    private static File getOrFetchAndWriteImageFile(String url) {
        final File file = getFile(url);
        byte[] bytes;
        if (file == null || !file.exists()) {
            bytes = Utils.getByteArrayFromImageURL(url); // blocking network operation
            if (bytes != null) {
                if (file != null && bytes.length < MAX_BITMAP_SIZE) {
                    OutputStream out = null;
                    try {
                        out = new FileOutputStream(file);
                        out.write(bytes);
                    } catch (FileNotFoundException e) {
                        Logger.v("CleverTap.ImageCache: error writing image file", e);
                        return null;
                    } catch (IOException e) {
                        Logger.v("CleverTap.ImageCache: error writing image file", e);
                        return null;
                    } finally {
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e) {
                                Logger.v("CleverTap.ImageCache: error closing image output file", e);
                            }
                        }
                    }
                }
            }
        }
        return file;
    }

    private static boolean isEmpty() {
        synchronized (ImageCache.class) {
            return mMemoryCache.size() <= 0;
        }
    }

    private static void removeFromFileSystem(String url) {
        final File file = getFile(url);
        if (file != null && file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }
}
