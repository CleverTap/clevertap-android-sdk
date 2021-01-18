package com.clevertap.android.sdk;

class CoreState extends CleverTapState {

    private BaseDatabaseManager mDatabaseManager;

    private BaseNetworkManager mNetworkManager;

    private PostAsyncSafelyHandler mPostAsyncSafelyHandler;

    @Override
    BaseDatabaseManager getDatabaseManager() {
        return mDatabaseManager;
    }

    @Override
    void setDatabaseManager(final BaseDatabaseManager databaseManager) {
        mDatabaseManager = databaseManager;
    }

    @Override
    BaseNetworkManager getNetworkManager() {
        return mNetworkManager;
    }

    @Override
    void setNetworkManager(final BaseNetworkManager networkManager) {
        mNetworkManager = networkManager;
    }

    @Override
    PostAsyncSafelyHandler getPostAsyncSafelyHandler() {
        return mPostAsyncSafelyHandler;
    }

    @Override
    void setPostAsyncSafelyHandler(final PostAsyncSafelyHandler postAsyncSafelyHandler) {
        mPostAsyncSafelyHandler = postAsyncSafelyHandler;
    }
}
