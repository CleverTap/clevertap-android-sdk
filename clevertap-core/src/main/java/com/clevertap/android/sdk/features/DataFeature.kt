package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.LocalDataStore
import com.clevertap.android.sdk.StoreProvider
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry

/**
 * Data storage and persistence layer
 * Contains database manager, local data store, and store registry
 */
internal data class DataFeature(
    val databaseManager: BaseDatabaseManager,
    val localDataStore: LocalDataStore,
    val storeRegistry: StoreRegistry,
    val storeProvider: StoreProvider
)
