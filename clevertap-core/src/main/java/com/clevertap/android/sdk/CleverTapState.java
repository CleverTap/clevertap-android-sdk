package com.clevertap.android.sdk;

abstract class CleverTapState {

    abstract BaseDatabaseManager getDatabaseManager();

    abstract void setDatabaseManager(final BaseDatabaseManager databaseManager);

    abstract BaseNetworkManager getNetworkManager();

    abstract void setNetworkManager(final BaseNetworkManager networkManager);

    abstract PostAsyncSafelyHandler getPostAsyncSafelyHandler();

    abstract void setPostAsyncSafelyHandler(final PostAsyncSafelyHandler postAsyncSafelyHandler);

}
