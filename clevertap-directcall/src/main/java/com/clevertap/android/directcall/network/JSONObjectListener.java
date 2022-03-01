package com.clevertap.android.directcall.network;

import androidx.annotation.Nullable;

import org.json.JSONObject;

public interface JSONObjectListener {
   void onResponse(@Nullable JSONObject res, int responseCode, Boolean isSuccessful);
   void onFailure(@Nullable Exception e);
}
