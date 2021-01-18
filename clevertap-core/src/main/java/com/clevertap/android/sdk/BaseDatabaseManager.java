package com.clevertap.android.sdk;

import android.content.Context;

abstract class BaseDatabaseManager {

    abstract void flushDBQueue(final Context context, final EventGroup eventGroup);
}
