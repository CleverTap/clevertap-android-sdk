package com.clevertap.android.sdk.pushnotification;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class PushType {

    @NonNull
    private final String ctProviderClassName;

    @NonNull
    private final String messagingSDKClassName;

    @NonNull
    private final String tokenPrefKey;

    @NonNull
    private final String type;

    public PushType(
            String type,
            String prefKey,
            String className,
            String messagingSDKClassName
    ) {
        this.type = type;
        this.tokenPrefKey = prefKey;
        this.ctProviderClassName = className;
        this.messagingSDKClassName = messagingSDKClassName;
    }

    @NonNull
    public String getCtProviderClassName() {
        return ctProviderClassName;
    }

    @NonNull
    public String getMessagingSDKClassName() {
        return messagingSDKClassName;
    }

    @NonNull
    public String getTokenPrefKey() {
        return tokenPrefKey;
    }

    @NonNull
    public String getType() {
        return type;
    }

    @NonNull
    @Override
    public String toString() {
        return " [PushType:" + type + "] ";
    }

    // Method to convert PushType to JSONObject
    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("ctProviderClassName", ctProviderClassName);
            jsonObject.put("messagingSDKClassName", messagingSDKClassName);
            jsonObject.put("tokenPrefKey", tokenPrefKey);
            jsonObject.put("type", type);
        } catch (JSONException e) {
            return null;
        }
        return jsonObject;
    }

    // Static method to convert JSONObject to PushType
    public static PushType fromJSONObject(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        try {
            String ctProviderClassName = jsonObject.getString("ctProviderClassName");
            String messagingSDKClassName = jsonObject.getString("messagingSDKClassName");
            String tokenPrefKey = jsonObject.getString("tokenPrefKey");
            String type = jsonObject.getString("type");
            return new PushType(type, tokenPrefKey, ctProviderClassName, messagingSDKClassName);
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PushType)) return false;
        PushType pushType = (PushType) o;
        return Objects.equals(ctProviderClassName, pushType.ctProviderClassName) && Objects.equals(messagingSDKClassName, pushType.messagingSDKClassName) && Objects.equals(tokenPrefKey, pushType.tokenPrefKey) && Objects.equals(type, pushType.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctProviderClassName, messagingSDKClassName, tokenPrefKey, type);
    }
}
