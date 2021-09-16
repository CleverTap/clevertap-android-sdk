package com.clevertap.android.sdk.interfaces;

import android.os.Bundle;
import androidx.annotation.NonNull;

public interface INotificationParser<T> {

    Bundle toBundle(@NonNull T message);
}
