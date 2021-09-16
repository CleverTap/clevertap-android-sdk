package com.clevertap.android.sdk.interfaces;

import android.content.Context;
import androidx.annotation.NonNull;

public interface IPushAmpHandler<T> {

    void processPushAmp(final Context context, @NonNull final T message);
}
