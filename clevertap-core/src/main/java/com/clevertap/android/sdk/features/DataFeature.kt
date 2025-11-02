package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.LocalDataStore
import com.clevertap.android.sdk.db.BaseDatabaseManager

/**
 * Data storage and persistence layer
 * Contains database manager, local data store
 *
 * Note: This encapsulates the entire SDK data operations via BaseDatabaseManager, eventually we
 * should break it down and move the reps to right place
 */
internal data class DataFeature(
    val databaseManager: BaseDatabaseManager,
    val localDataStore: LocalDataStore
)
