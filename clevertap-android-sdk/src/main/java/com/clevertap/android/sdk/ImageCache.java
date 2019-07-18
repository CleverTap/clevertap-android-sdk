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
    private static final int MIN_CACHE_SIZE = 1024 * 10; // 10mb minimum (in KB)
    private final static int maxMemory = (int) (Runtime.getRuntime().maxMemory())/1024;
    private final static int cacheSize = Math.max((maxMemory / 32), MIN_CACHE_SIZE);
    private static final String DIRECTORY_PREFIX = "CT.Images.";
    private static final String FILE_PREFIX = "CT_IMAGE_";
    private static final int MAX_BITMAP_SIZE = 10000000; // 10 MB
    private static LruCache<String, Bitmap> mMemoryCache;

    private File imageFileDirectory;
    private MessageDigest messageDigest;

    public ImageCache(Context context, String module){
        this.imageFileDirectory = context.getDir(DIRECTORY_PREFIX+module,Context.MODE_PRIVATE);
        MessageDigest messageDigest;
        try{
            messageDigest = MessageDigest.getInstance("SHA1");
        }catch (NoSuchAlgorithmException e) {
            Logger.d("Images won't be stored because this platform doesn't supply a SHA1 hash function");
            messageDigest = null;
        }
        this.messageDigest = messageDigest;
        init();
    }

    static void init(){
        synchronized (ImageCache.class) {
            if(mMemoryCache == null) {
                Logger.v("CTInAppNotification.ImageCache: init with max device memory: " + String.valueOf(maxMemory) + "KB and allocated cache size: " + String.valueOf(cacheSize) + "KB");
                try {
                    mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
                        @Override
                        protected int sizeOf(String key, Bitmap bitmap) {
                            // The cache size will be measured in kilobytes rather than
                            // number of items.
                            int size = getImageSizeInKB(bitmap);
                            Logger.v( "CTInAppNotification.ImageCache: have image of size: "+size + "KB for key: " + key);
                            return size;
                        }
                    };
                } catch (Throwable t) {
                    Logger.v( "CTInAppNotification.ImageCache: unable to initialize cache: ", t.getCause());
                }
            }
        }
    }

    private static int getImageSizeInKB(Bitmap bitmap) {
        return bitmap.getByteCount() / 1024;
    }

    private static int getAvailableMemory() {
        synchronized (ImageCache.class) {
            return mMemoryCache == null ? 0 : cacheSize - mMemoryCache.size();
        }
    }

    private static boolean isEmpty() {
        synchronized (ImageCache.class) {
            return mMemoryCache.size() <= 0;
        }
    }

    private static void cleanup() {
        synchronized (ImageCache.class) {
            if (isEmpty()) {
                Logger.v( "CTInAppNotification.ImageCache: cache is empty, removing it");
                mMemoryCache = null;
            }
        }
    }

    static boolean addBitmap(String key, Bitmap bitmap) {

        if(mMemoryCache==null) return false;

        if (getBitmap(key) == null) {
            synchronized (ImageCache.class) {
                int imageSize = getImageSizeInKB(bitmap);
                int available = getAvailableMemory();
                Logger.v( "CTInAppNotification.ImageCache: image size: "+ imageSize +"KB. Available mem: "+available+ "KB.");
                if (imageSize > getAvailableMemory()) {
                    Logger.v( "CTInAppNotification.ImageCache: insufficient memory to add image: " + key);
                    return false;
                }
                mMemoryCache.put(key, bitmap);
                Logger.v( "CTInAppNotification.ImageCache: added image for key: " + key);
            }
        }
        return true;
    }

    static Bitmap getBitmap(String key) {
        synchronized (ImageCache.class) {
            if(key!=null)
                return mMemoryCache == null ? null : mMemoryCache.get(key);
            else
                return null;
        }
    }

    static void removeBitmap(String key) {
        synchronized (ImageCache.class) {
            if (mMemoryCache == null) return;
            mMemoryCache.remove(key);
            Logger.v( "CTInAppNotification.LruImageCache: removed image for key: " + key);
            cleanup();
        }
    }

    public Bitmap getImageBitmap(String key, String url){
        Bitmap cachedBitmap = getBitmap(key);

        if(cachedBitmap == null){
            final File imageFile = getImageFile(url);
            if(imageFile != null) {
                cachedBitmap = decodeImage(imageFile);
                addBitmap(key, cachedBitmap);
            }else{
                return null;
            }
        }
        return cachedBitmap;
    }

    public File getImageFile(String url) {
        final File file = getEmptyFile(url);
        byte[] bytes = null;

        if (file == null || !file.exists()) {

            bytes = Utils.getByteArrayFromImageURL(url);

            if (null != bytes) {
                if (null != file && bytes.length < MAX_BITMAP_SIZE) {
                    OutputStream out = null;
                    try {
                        out = new FileOutputStream(file);
                        out.write(bytes);
                    } catch (FileNotFoundException e) {
                        //TODO logging
                        return null;
                    } catch (IOException e) {
                        //TODO logging
                        return null;
                    } finally {
                        if (null != out) {
                            try {
                                out.close();
                            } catch (IOException e) {
                                Logger.v( "Problem closing output file", e);
                            }
                        }
                    }
                }
            }
        }
        return file;
    }

    private File getEmptyFile(String url) {
        if (null == messageDigest) {
            return null;
        }

        final byte[] hashed = messageDigest.digest(url.getBytes());
        final String safeName = FILE_PREFIX + Base64.encodeToString(hashed, Base64.URL_SAFE | Base64.NO_WRAP);
        return new File(imageFileDirectory, safeName);
    }

    private static Bitmap decodeImage(File file) {
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), option);
        float imageSize = (float) option.outHeight * option.outWidth * 4; // 4 bytes per pixel
        if (imageSize > getAvailableMemory()) {
            return null;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (null == bitmap) {
            final boolean ignored = file.delete();
        }

        return bitmap;
    }
}
