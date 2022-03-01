package com.clevertap.android.directcall.network;

import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class HttpUtils {
    public static final HttpUtils INSTANCE;

    private HttpUtils() {
    }

    static {
        INSTANCE = new HttpUtils();
    }

    public final String asString(@Nullable byte[] data) {
        if (data == null || data.length == 0)
            return "";
        else
            return new String(data, StandardCharsets.UTF_8);
    }

    public final int checkTimeoutDuration(String name, long timeout, TimeUnit unit){
        Preconditions.checkArgument(timeout >= 0, name +  " < 0");
        Preconditions.checkArgument(unit != null, "TimeUnit == null");
        int millis = (int) unit.toMillis(timeout);
        Preconditions.checkArgument(millis <= Integer.MAX_VALUE, name + " too large.");
        return millis;
    }
}
