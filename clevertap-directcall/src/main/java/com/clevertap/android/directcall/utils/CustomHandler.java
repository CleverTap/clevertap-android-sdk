package com.clevertap.android.directcall.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.IntDef;

import com.clevertap.android.directcall.exception.CallException;
import com.clevertap.android.directcall.exception.DirectCallException;
import com.clevertap.android.directcall.exception.InitException;
import com.clevertap.android.directcall.interfaces.DirectCallInitResponse;
import com.clevertap.android.directcall.interfaces.OutgoingCallResponse;
import com.clevertap.android.directcall.javaclasses.VoIPCallStatus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class CustomHandler {
    private static CustomHandler instance = null;

    public static CustomHandler getInstance() {
        if (instance == null) {
            instance = new CustomHandler();
        }
        return instance;
    }

    private CustomHandler() {
    }

    @IntDef({OutCall.CALL_STATUS, OutCall.ON_SUCCESS, OutCall.ON_FAILURE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface OutCall {
        int CALL_STATUS = 1;
        int ON_SUCCESS = 2;
        int ON_FAILURE = 3;
    }

    @IntDef({Init.ON_SUCCESS, Init.ON_FAILURE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Init {
        int ON_SUCCESS = 1;
        int ON_FAILURE = 2;
    }

    public void sendCallAnnotation(final OutgoingCallResponse outgoingCallResponse, @OutCall final int callbackType, DirectCallException directCallException, VoIPCallStatus voIPCallStatus) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    switch (callbackType) {
                        case OutCall.CALL_STATUS:
                            outgoingCallResponse.callStatus(voIPCallStatus);
                            break;
                        case OutCall.ON_SUCCESS:
                            outgoingCallResponse.onSuccess();
                            break;
                        case OutCall.ON_FAILURE:
                            if(directCallException !=null){
                                int errorCode = directCallException.getErrorCode();
                                String errorMessage = directCallException.getMessage();
                                String errorExplanation = directCallException.getExplanation();
                                CallException callException = new CallException(errorCode, errorMessage, errorExplanation);
                                outgoingCallResponse.onFailure(callException);
                            }
                            break;
                    }
                }catch (Exception e){
                    //no-op
                }
            }
        });
    }

    public void sendInitAnnotations(final DirectCallInitResponse directCallInitResponse, @Init final int callbackType, final DirectCallException directCallException) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    switch (callbackType) {
                        case Init.ON_SUCCESS:
                            directCallInitResponse.onSuccess();
                            break;
                        case Init.ON_FAILURE:
                            if(directCallException !=null){
                                int errorCode = directCallException.getErrorCode();
                                String errorMessage = directCallException.getMessage();
                                String errorExplanation = directCallException.getExplanation();
                                InitException initException = new InitException(errorCode, errorMessage, errorExplanation);
                                directCallInitResponse.onFailure(initException);
                            }
                            break;
                    }
                }catch (Exception e){
                    //no-op
                }
            }
        });
    }
}
