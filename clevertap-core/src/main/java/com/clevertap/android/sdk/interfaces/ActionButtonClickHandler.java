package com.clevertap.android.sdk.interfaces;

import android.os.Bundle;

public interface ActionButtonClickHandler extends NotificationHandler {

    String getType(Bundle extras);

    boolean onActionButtonClick();
}
