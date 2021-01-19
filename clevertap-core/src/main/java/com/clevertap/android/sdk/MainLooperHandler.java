package com.clevertap.android.sdk;

import android.os.Handler;
import android.os.Looper;

//ToDO move this a single Task manager
class MainLooperHandler extends Handler{
    MainLooperHandler(){
        super(Looper.myLooper());
    }
}