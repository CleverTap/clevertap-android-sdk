package com.clevertap.android.sdk.interfaces;

import android.content.Context;
import android.os.Bundle;

public interface ActionButtonClickHandler extends NotificationHandler {

    boolean onActionButtonClick(Context context, Bundle extras, int notificationId);
}
