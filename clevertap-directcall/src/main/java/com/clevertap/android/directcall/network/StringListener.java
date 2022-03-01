package com.clevertap.android.directcall.network;

import androidx.annotation.Nullable;

public interface StringListener {
   void onResponse(@Nullable String res, int responseCode, Boolean isSuccessful);
   void onFailure(@Nullable Exception e);
}
