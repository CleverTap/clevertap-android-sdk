package com.clevertap.android.sdk;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class NullObjectFactory {

    public static <T> T dummyObject(Class<T> className) {
        try {
            return className.newInstance();
        } catch (Throwable throwable) {
            return null;
        }
    }

    private NullObjectFactory() {
        // Util class
    }
}
