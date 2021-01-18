package com.clevertap.android.sdk;

import android.content.Context;

abstract class CleverTapState {

    private final Context context;

    CleverTapState(final Context context) {
        this.context = context;
    }

    Context getContext() {
        return context;
    }

    abstract BaseDatabaseManager getDatabaseManager();

    abstract void setDatabaseManager(final BaseDatabaseManager databaseManager);

    abstract BaseNetworkManager getNetworkManager();

    abstract void setNetworkManager(final BaseNetworkManager networkManager);

    abstract PostAsyncSafelyHandler getPostAsyncSafelyHandler();

    abstract void setPostAsyncSafelyHandler(final PostAsyncSafelyHandler postAsyncSafelyHandler);

}
