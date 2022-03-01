package com.clevertap.android.directcall.network;

import androidx.annotation.Nullable;

import org.json.JSONArray;

public interface JSONArrayListener {
   void onResponse(@Nullable JSONArray res, int responseCode, Boolean isSuccessful);
   void onFailure(@Nullable Exception e);
}
