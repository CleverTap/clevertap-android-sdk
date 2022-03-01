package com.clevertap.android.directcall.interfaces;

import androidx.annotation.NonNull;
import com.clevertap.android.directcall.exception.InitException;

public interface DirectCallInitResponse {
    void onSuccess();
    void onFailure(@NonNull InitException initException);
}
