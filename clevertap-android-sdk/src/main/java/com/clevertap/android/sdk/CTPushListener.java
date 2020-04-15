package com.clevertap.android.sdk;

import android.os.Bundle;

public interface CTPushListener {
    void onPushPayloadReceived(Bundle extras);
}
