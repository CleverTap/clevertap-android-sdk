package com.clevertap.android.sdk.pushnotification;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class PushType {

    private final String ctProviderClassName;

    private final String messagingSDKClassName;

    private final String tokenPrefKey;

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

    public String getCtProviderClassName() {
        return ctProviderClassName;
    }

    public String getMessagingSDKClassName() {
        return messagingSDKClassName;
    }

    public String getTokenPrefKey() {
        return tokenPrefKey;
    }

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
}
