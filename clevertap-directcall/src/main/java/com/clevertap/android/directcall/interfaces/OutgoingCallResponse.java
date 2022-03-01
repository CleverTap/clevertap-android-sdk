package com.clevertap.android.directcall.interfaces;

import com.clevertap.android.directcall.exception.CallException;
import com.clevertap.android.directcall.javaclasses.VoIPCallStatus;

public interface OutgoingCallResponse {
    void callStatus(VoIPCallStatus voIPCallStatus);
    void onSuccess();
    void onFailure(CallException callException);
}
