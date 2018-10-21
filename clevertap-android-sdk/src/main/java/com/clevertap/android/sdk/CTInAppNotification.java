package com.clevertap.android.sdk;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.LruCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

class CTInAppNotification implements Parcelable {

    private String id;
    private String campaignId;
    private String type;
    private CTInAppType inAppType;

    private String html;
    private boolean excludeFromCaps;
    private boolean showClose;
    private boolean darkenScreen;
    private int maxPerSession;
    private int totalLifetimeCount;
    private int totalDailyCount;
    private char position;
    private int height;
    private int heightPercentage;
    private int width;
    private int widthPercentage;

    private String imageUrl;
    private String _imageCacheKey;
    private String contentType;
    private String mediaUrl;

    private String title;
    private String titleColor;
    private String message;
    private String messageColor;
    private String backgroundColor;

    private boolean hideCloseButton;

    private ArrayList<CTInAppNotificationButton> buttons = new ArrayList<>();
    private int buttonCount;

    private JSONObject jsonDescription;
    private String error;
    private JSONObject customExtras;
    private JSONObject actionExtras;
    CTInAppNotificationListener listener;


    CTInAppNotification(){}

    interface CTInAppNotificationListener{
        void notificationReady(CTInAppNotification inAppNotification);
    }

    CTInAppNotification initWithJSON(JSONObject jsonObject){
        this.jsonDescription = jsonObject;
        try {

            this.type = jsonObject.has("type") ? jsonObject.getString("type") : null;

            if(this.type !=null ) {
                if(this.type.equals("custom-html")) {
                    legacyConfigureWithJson(jsonObject);
                }else {
                    configureWithJson(jsonObject);
                }
            }else{
                legacyConfigureWithJson(jsonObject);
            }

        } catch (JSONException e) {
            this.error = "Invalid JSON : "+e.getLocalizedMessage();
        }
        return this;

    }

    private void legacyConfigureWithJson(JSONObject jsonObject){
        Bundle b = getBundleFromJsonObject(jsonObject);
        if(!validateNotifBundle(b)){
            this.error = "Invalid JSON";
            return;
        }
        try {
            this.id = jsonObject.has(Constants.INAPP_ID_IN_PAYLOAD) ? jsonObject.getString(Constants.INAPP_ID_IN_PAYLOAD) : "";
            this.campaignId = jsonObject.has(Constants.NOTIFICATION_ID_TAG) ? jsonObject.getString(Constants.NOTIFICATION_ID_TAG) : "";
            this.excludeFromCaps = jsonObject.has("efc") && jsonObject.getInt("efc") == 1;
            this.totalLifetimeCount = jsonObject.has("tlc") ? jsonObject.getInt("tlc") : -1;
            this.totalDailyCount = jsonObject.has("tdc") ? jsonObject.getInt("tdc") : -1;

            JSONObject data = jsonObject.has("d") ? jsonObject.getJSONObject("d") : null;
            if (data != null) {
                this.html = data.getString(Constants.INAPP_DATA_TAG);

                this.customExtras = data.has("kv") ? data.getJSONObject("kv") : null;
                if (this.customExtras == null)
                    this.customExtras = new JSONObject();

                JSONObject displayParams = jsonObject.getJSONObject(Constants.INAPP_WINDOW);
                if (displayParams != null) {
                    this.darkenScreen = displayParams.getBoolean(Constants.INAPP_NOTIF_DARKEN_SCREEN);
                    this.showClose = displayParams.getBoolean(Constants.INAPP_NOTIF_SHOW_CLOSE);
                    this.position = displayParams.getString(Constants.INAPP_POSITION).charAt(0);
                    this.width = displayParams.has(Constants.INAPP_X_DP) ? displayParams.getInt(Constants.INAPP_X_DP) : 0;
                    this.widthPercentage = displayParams.has(Constants.INAPP_X_PERCENT) ? displayParams.getInt(Constants.INAPP_X_PERCENT) : 0;
                    this.height = displayParams.has(Constants.INAPP_Y_DP) ? displayParams.getInt(Constants.INAPP_Y_DP) : 0;
                    this.heightPercentage = displayParams.has(Constants.INAPP_Y_PERCENT) ? displayParams.getInt(Constants.INAPP_Y_PERCENT) : 0;
                    this.maxPerSession = displayParams.has(Constants.INAPP_MAX_DISPLAY_COUNT) ? displayParams.getInt(Constants.INAPP_MAX_DISPLAY_COUNT) : -1;
                }

                if (this.html != null) {
                    if (this.position == 't' && this.widthPercentage == 100 && this.heightPercentage == 30)
                        this.inAppType = CTInAppType.CTInAppTypeHeaderHTML;
                    else if (this.position == 'b' && this.widthPercentage == 100 && this.heightPercentage == 30)
                        this.inAppType = CTInAppType.CTInAppTypeFooterHTML;
                    else if (this.position == 'c' && this.widthPercentage == 90 && this.heightPercentage == 85)
                        this.inAppType = CTInAppType.CTInAppTypeInterstitialHTML;
                    else if (this.position == 'c' && this.widthPercentage == 100 && this.heightPercentage == 100)
                        this.inAppType = CTInAppType.CTInAppTypeCoverHTML;
                    else if (this.position == 'c' && this.widthPercentage == 90 && this.heightPercentage == 50)
                        this.inAppType = CTInAppType.CTInAppTypeHalfInterstitialHTML;
                }
            }
        }catch (JSONException e){
            this.error = "Invalid JSON";
        }
    }

    private void configureWithJson(JSONObject jsonObject){
        try {
            this.campaignId = jsonObject.has(Constants.NOTIFICATION_ID_TAG) ? jsonObject.getString(Constants.NOTIFICATION_ID_TAG) : "";
            this.type = jsonObject.getString("type");
            this.inAppType = CTInAppType.fromString(this.type);
            this.backgroundColor = jsonObject.has("bg") ? jsonObject.getString("bg") : "";
            JSONObject titleObject = jsonObject.has("title") ? jsonObject.getJSONObject("title") : null;
            if(titleObject != null) {
                this.title = titleObject.has("text") ? titleObject.getString("text") : "";
                this.titleColor = titleObject.has("color") ? titleObject.getString("color") : "";
            }
            JSONObject msgObject = jsonObject.has("message") ? jsonObject.getJSONObject("message") : null;
            if(msgObject != null) {
                this.message = msgObject.has("text") ? msgObject.getString("text") : "";
                this.messageColor = msgObject.has("color") ? msgObject.getString("color") : "";
            }
            this.hideCloseButton = jsonObject.has("close") ? jsonObject.getBoolean("close") : false;
            JSONObject media = jsonObject.has("media") ? jsonObject.getJSONObject("media") : null;
            if(media!=null){
                this.contentType = media.has("content_type") ? media.getString("content_type") : "";
                String mediaUrl = media.has("url") ? media.getString("url") : "";
                if(!mediaUrl.isEmpty()){
                    if(this.contentType.startsWith("image")){
                        this.imageUrl = mediaUrl;
                        this._imageCacheKey = media.has("key") ? media.getString("key") : "";
                    } else {
                        this.mediaUrl = mediaUrl;
                    }
                }
            }
            JSONArray buttonArray = jsonObject.has("buttons") ? jsonObject.getJSONArray("buttons") : null;
            if(buttonArray  != null) {
                for (int i = 0; i < buttonArray.length(); i++) {
                    CTInAppNotificationButton inAppNotificationButton = new CTInAppNotificationButton().initWithJSON(buttonArray.getJSONObject(i));
                    if (inAppNotificationButton != null && inAppNotificationButton.getError() == null) {
                        this.buttons.add(inAppNotificationButton);
                        this.buttonCount++;
                    }
                }
            }
        }catch (JSONException e){
            this.error = "Invalid JSON"+e.getLocalizedMessage();
        }
    }

    void prepareForDisplay() {

        if (this.mediaIsGIF()) {
            GifCache.init();
            if(this.getGifByteArray() != null){
                listener.notificationReady(this);
                return;
            }
            Logger.v("CTInAppNotification: downloading GIF :" + this.imageUrl);
            byte[] gifByteArray = Utils.getByteArrayFromImageURL(this.imageUrl);
            if(gifByteArray != null){
                Logger.v("GIF Downloaded from url: " + this.imageUrl);
                if(!GifCache.addByteArray(this.getImageCacheKey(), gifByteArray)){
                    this.error = "Error processing GIF";
                }
                listener.notificationReady(this);
            }
        } else if (this.mediaIsImage()) {
            ImageCache.init();
            if (this.getImage() != null) {
                listener.notificationReady(this);
                return;
            }
            Logger.v("CTInAppNotification: downloading Image :" + this.imageUrl);
            Bitmap imageBitmap = Utils.getBitmapFromURL(this.imageUrl);
            if (imageBitmap != null) {
                Logger.v("Image Downloaded from url: " + this.imageUrl);
                if (!ImageCache.addBitmap(this.getImageCacheKey(), imageBitmap)) {
                    this.error = "Error processing image";
                }
                listener.notificationReady(this);
            }
        } else if(this.mediaIsVideo() || this.mediaIsAudio()){
            Class className = null;
            try{
                className = Class.forName("com.google.android.exoplayer2.ExoPlayerFactory");
                className = Class.forName("com.google.android.exoplayer2.source.hls.HlsMediaSource");
                className = Class.forName("com.google.android.exoplayer2.ui.PlayerView");
            }catch (Throwable t){
                Logger.d("ExoPlayer library files are missing!!!");
                Logger.d("Please add ExoPlayer dependencies to render In-App notifications playing audio/video. For more information checkout CleverTap documentation.");
                if(className!=null)
                    this.error = "Error finding ExoPlayer"+className.getName();
                else
                    this.error = "Error finding ExoPlayer";
            }
            listener.notificationReady(this);
        }else {
            listener.notificationReady(this);
        }
    }

    private boolean validateNotifBundle(Bundle notif) {
        try {
            final Bundle w = notif.getBundle(Constants.INAPP_WINDOW);
            final Bundle d = notif.getBundle("d");
            if (w == null || d == null) return false;

            // Check that either xdp or xp is set
            if (!isKeyValid(w, Constants.INAPP_X_DP, Integer.class))
                if (!isKeyValid(w, Constants.INAPP_X_PERCENT, Integer.class))
                    return false;

            // Check that either ydp or yp is set
            if (!isKeyValid(w, Constants.INAPP_Y_DP, Integer.class))
                if (!isKeyValid(w, Constants.INAPP_Y_PERCENT, Integer.class))
                    return false;

            // Check that dk is set
            if (!(isKeyValid(w, Constants.INAPP_NOTIF_DARKEN_SCREEN, Boolean.class)))
                return false;

            // Check that sc is set
            if (!(isKeyValid(w, Constants.INAPP_NOTIF_SHOW_CLOSE, Boolean.class)))
                return false;

            // Check that html is set
            if (!(isKeyValid(d, Constants.INAPP_DATA_TAG, String.class)))
                return false;

            // Check that pos contains the right value
            if ((isKeyValid(w, Constants.INAPP_POSITION, String.class))) {
                //noinspection ConstantConditions
                char pos = w.getString(Constants.INAPP_POSITION).charAt(0);
                switch (pos) {
                    case Constants.INAPP_POSITION_TOP:
                        break;
                    case Constants.INAPP_POSITION_RIGHT:
                        break;
                    case Constants.INAPP_POSITION_BOTTOM:
                        break;
                    case Constants.INAPP_POSITION_LEFT:
                        break;
                    case Constants.INAPP_POSITION_CENTER:
                        break;
                    default:
                        return false;
                }
            } else
                return false;

            return true;
        } catch (Throwable t) {
            Logger.v("Failed to parse in-app notification!", t);
            return false;
        }
    }

    private boolean isKeyValid(Bundle b, String key, Class<?> type) {
        //noinspection ConstantConditions
        return b.containsKey(key) && b.get(key).getClass().equals(type);
    }

    private static Bundle getBundleFromJsonObject(JSONObject notif) {
        Bundle b = new Bundle();
        Iterator iterator = notif.keys();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            try {
                Object value = notif.get(key);
                if (value instanceof String)
                    b.putString(key, (String) value);
                else if (value instanceof Character)
                    b.putChar(key, (Character) value);
                else if (value instanceof Integer)
                    b.putInt(key, (Integer) value);
                else if (value instanceof Float)
                    b.putFloat(key, (Float) value);
                else if (value instanceof Double)
                    b.putDouble(key, (Double) value);
                else if (value instanceof Long)
                    b.putLong(key, (Long) value);
                else if (value instanceof Boolean)
                    b.putBoolean(key, (Boolean) value);
                else if (value instanceof JSONObject)
                    b.putBundle(key, getBundleFromJsonObject((JSONObject) value));
            } catch (JSONException e) {
                Logger.v("Key had unknown object. Discarding");
            }
        }
        return b;
    }

    String getId() {
        return id;
    }

    String getCampaignId() {
        return campaignId;
    }

    public CTInAppType getInAppType() {
        return inAppType;
    }

    String getHtml() {
        return html;
    }

    boolean isExcludeFromCaps() {
        return excludeFromCaps;
    }

    boolean isShowClose() {
        return showClose;
    }

    boolean isDarkenScreen() {
        return darkenScreen;
    }

    int getMaxPerSession() {
        return maxPerSession;
    }

    int getTotalLifetimeCount() {
        return totalLifetimeCount;
    }

    int getTotalDailyCount() {
        return totalDailyCount;
    }

    char getPosition() {
        return position;
    }

    int getHeight() {
        return height;
    }

    int getHeightPercentage() {
        return heightPercentage;
    }

    int getWidth() {
        return width;
    }

    int getWidthPercentage() {
        return widthPercentage;
    }

    JSONObject getJsonDescription() {
        return jsonDescription;
    }

    String getError() {
        return error;
    }

    JSONObject getCustomExtras() {
        return customExtras;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public JSONObject getActionExtras() {
        return actionExtras;
    }

    String getType() {
        return type;
    }

    String getMediaUrl() {
        return mediaUrl;
    }

    String getTitle() {
        return title;
    }

    String getTitleColor() {
        return titleColor;
    }

    String getMessage() {
        return message;
    }

    String getMessageColor() {
        return messageColor;
    }

    String getContentType() {
        return contentType;
    }

    String getBackgroundColor() {
        return backgroundColor;
    }

    ArrayList<CTInAppNotificationButton> getButtons() {
        return buttons;
    }

    Bitmap getImage() {
        return ImageCache.getBitmap(getImageCacheKey());
    }

    byte[] getGifByteArray(){
        return GifCache.getByteArray(getImageCacheKey());
    }

    private String getImageCacheKey() {
        return this._imageCacheKey;
    }

    void didDismiss() {
        removeImageOrGif();
    }

    private void removeImageOrGif(){
        if (this.imageUrl != null) {
            if(!this.contentType.equals("image/gif")) {
                ImageCache.removeBitmap(getImageCacheKey());
            }else {
                GifCache.removeByteArray(getImageCacheKey());
            }
        }
    }

    String getImageUrl() {
        return imageUrl;
    }


    boolean isHideCloseButton() {
        return hideCloseButton;
    }

    int getButtonCount() {
        return buttonCount;
    }

    boolean mediaIsImage() {
        String contentType = this.getContentType();
        return contentType != null && this.imageUrl != null && contentType.startsWith("image") && !contentType.equals("image/gif");
    }

    boolean mediaIsGIF () {
        String contentType = this.getContentType();
        return contentType != null && this.imageUrl != null && contentType.equals("image/gif");
    }

    boolean mediaIsVideo () {
        String contentType = this.getContentType();
        return contentType != null && this.mediaUrl != null && contentType.startsWith("video");
    }

    boolean mediaIsAudio () {
        String contentType = this.getContentType();
        return contentType != null && this.mediaUrl != null && contentType.startsWith("audio");
    }

    private CTInAppNotification(Parcel in) {
        try {
            id = in.readString();
            campaignId = in.readString();
            inAppType = (CTInAppType) in.readValue(CTInAppType.class.getClassLoader());
            html = in.readString();
            excludeFromCaps = in.readByte() != 0x00;
            showClose = in.readByte() != 0x00;
            darkenScreen = in.readByte() != 0x00;
            maxPerSession = in.readInt();
            totalLifetimeCount = in.readInt();
            totalDailyCount = in.readInt();
            position = (char) in.readValue(char.class.getClassLoader());
            height = in.readInt();
            heightPercentage = in.readInt();
            width = in.readInt();
            widthPercentage = in.readInt();
            jsonDescription = in.readByte() == 0x00 ? null : new JSONObject(in.readString());
            error = in.readString();
            customExtras = in.readByte() == 0x00 ? null : new JSONObject(in.readString());
            actionExtras = in.readByte() == 0x00 ? null : new JSONObject(in.readString());
            type = in.readString();
            contentType = in.readString();
            imageUrl = in.readString();
            _imageCacheKey = in.readString();
            mediaUrl = in.readString();
            title = in.readString();
            titleColor = in.readString();
            backgroundColor = in.readString();
            message = in.readString();
            messageColor = in.readString();
            try {
                buttons = (ArrayList<CTInAppNotificationButton>) in.createTypedArrayList(CTInAppNotificationButton.CREATOR);
            } catch (Throwable t) {
                // no-op
            }
            hideCloseButton = in.readByte() != 0x00;
            buttonCount = in.readInt();

        }catch (JSONException e){
            // no-op
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(campaignId);
        dest.writeValue(inAppType);
        dest.writeString(html);
        dest.writeByte((byte) (excludeFromCaps ? 0x01 : 0x00));
        dest.writeByte((byte) (showClose ? 0x01 : 0x00));
        dest.writeByte((byte) (darkenScreen ? 0x01 : 0x00));
        dest.writeInt(maxPerSession);
        dest.writeInt(totalLifetimeCount);
        dest.writeInt(totalDailyCount);
        dest.writeValue(position);
        dest.writeInt(height);
        dest.writeInt(heightPercentage);
        dest.writeInt(width);
        dest.writeInt(widthPercentage);
        if (jsonDescription == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeString(jsonDescription.toString());
        }
        dest.writeString(error);
        if (customExtras == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeString(customExtras.toString());
        }
        if (actionExtras == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeString(actionExtras.toString());
        }
        dest.writeString(type);
        dest.writeString(contentType);
        dest.writeString(imageUrl);
        dest.writeString(_imageCacheKey);
        dest.writeString(mediaUrl);
        dest.writeString(title);
        dest.writeString(titleColor);
        dest.writeString(backgroundColor);
        dest.writeString(message);
        dest.writeString(messageColor);
        dest.writeTypedList(buttons);
        dest.writeByte((byte) (hideCloseButton ? 0x01 : 0x00));
        dest.writeInt(buttonCount);

    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<CTInAppNotification> CREATOR = new Parcelable.Creator<CTInAppNotification>() {
        @Override
        public CTInAppNotification createFromParcel(Parcel in) {
            return new CTInAppNotification(in);
        }

        @Override
        public CTInAppNotification[] newArray(int size) {
            return new CTInAppNotification[size];
        }
    };

    // intended to only hold an image reference for the life of the parent CTInAppNotification, in order to facilitate parceling
    private static class ImageCache {
        private static final int MIN_CACHE_SIZE = 1024 * 3; // 3mb minimum (in KB)  // TODO coordinate this minimum with the max gif size we will allow on the dashboard
        private final static int maxMemory = (int) (Runtime.getRuntime().maxMemory())/1024;
        private final static int cacheSize = Math.max((maxMemory / 32), MIN_CACHE_SIZE);

        private static LruCache<String, Bitmap> mMemoryCache;

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
                return mMemoryCache == null ? null : mMemoryCache.get(key);
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
    }

    // intended to only hold an gif byte array reference for the life of the parent CTInAppNotification, in order to facilitate parceling
    private static class GifCache {
        private static final int MIN_CACHE_SIZE = 1024 * 5; // 5mb minimum (in KB)  // TODO coordinate this minimum with the max gif size we will allow on the dashboard
        private final static int maxMemory = (int) (Runtime.getRuntime().maxMemory())/1024;
        private final static int cacheSize = Math.max((maxMemory / 32), MIN_CACHE_SIZE);

        private static LruCache<String, byte[]> mMemoryCache;

        static void init(){
            synchronized (GifCache.class) {
                if(mMemoryCache == null) {
                    Logger.v("CTInAppNotification.GifCache: init with max device memory: " + String.valueOf(maxMemory) + "KB and allocated cache size: " + String.valueOf(cacheSize) + "KB");
                    try {
                        mMemoryCache = new LruCache<String, byte[]>(cacheSize) {
                            @Override
                            protected int sizeOf(String key, byte[] byteArray) {
                                // The cache size will be measured in kilobytes rather than
                                // number of items.
                                int size = getByteArraySizeInKB(byteArray);
                                Logger.v( "CTInAppNotification.GifCache: have gif of size: "+size + "KB for key: " + key);
                                return size;
                            }
                        };
                    } catch (Throwable t) {
                        Logger.v( "CTInAppNotification.GifCache: unable to initialize cache: ", t.getCause());
                    }
                }
            }
        }

        private static int getByteArraySizeInKB(byte[] byteArray) {
            return byteArray.length / 1024;
        }

        private static int getAvailableMemory() {
            synchronized (GifCache.class) {
                return mMemoryCache == null ? 0 : cacheSize - mMemoryCache.size();
            }
        }

        private static boolean isEmpty() {
            synchronized (GifCache.class) {
                return mMemoryCache.size() <= 0;
            }
        }

        private static void cleanup() {
            synchronized (GifCache.class) {
                if (isEmpty()) {
                    Logger.v( "CTInAppNotification.GifCache: cache is empty, removing it");
                    mMemoryCache = null;
                }
            }
        }

        static boolean addByteArray(String key, byte[] byteArray) {

            if(mMemoryCache == null) return false;

            if (getByteArray(key) == null) {
                synchronized (GifCache.class) {
                    int arraySize = getByteArraySizeInKB(byteArray);
                    int available = getAvailableMemory();
                    Logger.v( "CTInAppNotification.GifCache: gif size: "+ arraySize +"KB. Available mem: "+available+"KB.");
                    if (arraySize > getAvailableMemory()) {
                        Logger.v( "CTInAppNotification.GifCache: insufficient memory to add gif: " + key);
                        return false;
                    }
                    mMemoryCache.put(key, byteArray);
                    Logger.v( "CTInAppNotification.GifCache: added gif for key: " + key);
                }
            }
            return true;
        }

        static byte[] getByteArray(String key) {
            synchronized (GifCache.class) {
                return mMemoryCache == null ? null : mMemoryCache.get(key);
            }
        }

        static void removeByteArray(String key) {
            synchronized (GifCache.class) {
                if (mMemoryCache == null) return;
                mMemoryCache.remove(key);
                Logger.v( "CTInAppNotification.GifCache: removed gif for key: " + key);
                cleanup();
            }
        }
    }
}
