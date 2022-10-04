package com.clevertap.android.sdk.inapp;


import android.content.Context;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.DeviceInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class CTLocalInAppBuilder {

    public static final String HALF_INTERSTITIAL_INAPP = "half-interstitial";
    public static final String ALERT_INAPP = "alert-template";
    protected static final String IS_LOCAL_INAPP = "isLocalInApp";

    private final String localInAppType;//Required
    private final JSONObject titleObj;// Required
    private final JSONObject messageObj;// Required
    private final boolean followDeviceOrientation;// Required
    private final String positiveBtnText;// Required
    private final String negativeBtnText;// Required

    CTLocalInAppBuilder(Builder.Builder1 builder) throws JSONException {
        this.localInAppType = builder.jsonObject.getString(Constants.KEY_TYPE);
        this.titleObj = builder.titleObject;
        this.messageObj = builder.msgObject;
        this.followDeviceOrientation = builder.jsonObject.getBoolean(Constants.KEY_LANDSCAPE);
        this.positiveBtnText = builder.buttonArray.get(0).toString();
        this.negativeBtnText = builder.buttonArray.get(1).toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        public Builder1 inAppType(String inAppType, Context context) throws JSONException {
            return new Builder1(inAppType, context);
        }

        public static final class Builder1 {

            JSONObject jsonObject = new JSONObject();
            JSONObject titleObject = new JSONObject();
            JSONObject msgObject = new JSONObject();
            JSONObject media = new JSONObject();
            JSONArray buttonArray = new JSONArray();
            JSONObject positiveButtonObject = new JSONObject();
            JSONObject negativeButtonObject = new JSONObject();

            String btnBorderRadius;

            private Builder1(String inAppType, Context context) throws JSONException {
                this.jsonObject.put(Constants.KEY_TYPE, inAppType);
                this.jsonObject.put(IS_LOCAL_INAPP,true);
                this.jsonObject.put(Constants.KEY_HIDE_CLOSE,true);
                this.jsonObject.put(Constants.KEY_IS_TABLET,
                        DeviceInfo.getDeviceType(context) == DeviceInfo.TABLET);

            }

            public Builder2 titleText(String titleText) throws JSONException {
                this.titleObject.put(Constants.KEY_TEXT, titleText);
                this.jsonObject.put(Constants.KEY_TITLE, titleObject);
                return new Builder2(Builder1.this);
            }
        }

        public static final class Builder2 {
            final Builder1 builder;

            private Builder2(Builder1 builder) {
                this.builder = builder;
            }

            public Builder3 messageText(String messageText) throws JSONException {
                this.builder.msgObject.put(Constants.KEY_TEXT, messageText);
                this.builder.jsonObject.put(Constants.KEY_MESSAGE, this.builder.msgObject);
                return new Builder3(this.builder);
            }
        }

        public static final class Builder3 {
            final Builder1 builder;

            private Builder3(Builder1 builder) {
                this.builder = builder;
            }

            public Builder4 followDeviceOrientation(boolean followDeviceOrientation) throws JSONException {
                this.builder.jsonObject.put(Constants.KEY_PORTRAIT, true);
                this.builder.jsonObject.put(Constants.KEY_LANDSCAPE, followDeviceOrientation);
                return new Builder4(this.builder);
            }
        }

        public static final class Builder4 {
            final Builder1 builder;

            private Builder4(Builder1 builder) {
                this.builder = builder;
            }

            public Builder5 positiveBtnText(String positiveBtnText) throws JSONException {
                this.builder.positiveButtonObject.put(Constants.KEY_TEXT, positiveBtnText);
                if (this.builder.btnBorderRadius == null || this.builder.btnBorderRadius.isEmpty()){
                    this.builder.positiveButtonObject.put(Constants.KEY_RADIUS, "2");
                }
                this.builder.buttonArray.put(0, this.builder.positiveButtonObject);
                this.builder.jsonObject.put(Constants.KEY_BUTTONS, this.builder.buttonArray);
                return new Builder5(this.builder);
            }
        }

        public static final class Builder5 {
            final Builder1 builder;

            private Builder5(Builder1 builder) {
                this.builder = builder;
            }

            public Builder6 negativeBtnText(String negativeBtnText) throws JSONException {
                this.builder.negativeButtonObject.put(Constants.KEY_TEXT, negativeBtnText);
                if (this.builder.btnBorderRadius == null || this.builder.btnBorderRadius.isEmpty()){
                    this.builder.negativeButtonObject.put(Constants.KEY_RADIUS, "2");
                }
                this.builder.buttonArray.put(1, this.builder.negativeButtonObject);
                this.builder.jsonObject.put(Constants.KEY_BUTTONS, this.builder.buttonArray);
                return new Builder6(this.builder);
            }
        }

        public static final class Builder6 {
            final Builder1 builder;

            private Builder6(Builder1 builder) {
                this.builder = builder;
            }

            public Builder6 backgroundColor(String backgroundColor) throws JSONException {
                this.builder.jsonObject.put(Constants.KEY_BG, backgroundColor);
                return this;
            }

            public Builder6 imageUrl(String imageUrl) throws JSONException {
                this.builder.media.put(Constants.KEY_URL, imageUrl);
                this.builder.media.put(Constants.KEY_CONTENT_TYPE, "image");
                this.builder.jsonObject.put(Constants.KEY_MEDIA, this.builder.media);

                if (builder.jsonObject.getBoolean(Constants.KEY_LANDSCAPE)) {
                    this.builder.media.put(Constants.KEY_URL, imageUrl);
                    this.builder.media.put(Constants.KEY_CONTENT_TYPE, "image");
                    this.builder.jsonObject.put(Constants.KEY_MEDIA_LANDSCAPE, this.builder.media);
                }
                return this;
            }

            public Builder6 titleTextColor(String titleTextColor) throws JSONException {
                this.builder.titleObject.put(Constants.KEY_COLOR, titleTextColor);
                this.builder.jsonObject.put(Constants.KEY_TITLE, this.builder.titleObject);
                return this;
            }

            public Builder6 messageTextColor(String messageTextColor) throws JSONException {
                this.builder.msgObject.put(Constants.KEY_COLOR, messageTextColor);
                this.builder.jsonObject.put(Constants.KEY_MESSAGE, this.builder.msgObject);
                return this;
            }

            public Builder6 btnTextColor(String btnTextColor) throws JSONException {
                this.builder.jsonObject.put(Constants.KEY_BUTTONS,
                        getActionButtonJSONArray(Constants.KEY_COLOR, btnTextColor));
                return this;
            }

            public Builder6 btnBackgroundColor(String btnBackgroundColor) throws JSONException {
                this.builder.jsonObject.put(Constants.KEY_BUTTONS,
                        getActionButtonJSONArray(Constants.KEY_BG, btnBackgroundColor));
                return this;
            }

            public Builder6 btnBorderColor(String btnBorderColor) throws JSONException {
                this.builder.jsonObject.put(Constants.KEY_BUTTONS,
                        getActionButtonJSONArray(Constants.KEY_BORDER, btnBorderColor));
                return this;
            }

            public Builder6 btnBorderRadius(String btnBorderRadius) throws JSONException {
                this.builder.btnBorderRadius = btnBorderRadius;
                this.builder.jsonObject.put(Constants.KEY_BUTTONS,
                        getActionButtonJSONArray(Constants.KEY_RADIUS, btnBorderRadius));
                return this;
            }
            public JSONObject build() {
                return this.builder.jsonObject;
            }


            private JSONArray getActionButtonJSONArray(String key, String value) throws JSONException{
                this.builder.positiveButtonObject.put(key, value);
                this.builder.negativeButtonObject.put(key, value);

                this.builder.buttonArray.put(0, this.builder.positiveButtonObject);
                this.builder.buttonArray.put(1, this.builder.negativeButtonObject);

                return this.builder.buttonArray;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CTLocalInAppBuilder that = (CTLocalInAppBuilder) o;
        return followDeviceOrientation == that.followDeviceOrientation &&
                Objects.equals(localInAppType, that.localInAppType) &&
                Objects.equals(titleObj, that.titleObj) && Objects.equals(messageObj, that.messageObj)
                && Objects.equals(positiveBtnText, that.positiveBtnText)
                && Objects.equals(negativeBtnText, that.negativeBtnText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localInAppType, titleObj, messageObj, followDeviceOrientation,
                positiveBtnText, negativeBtnText);
    }
}
