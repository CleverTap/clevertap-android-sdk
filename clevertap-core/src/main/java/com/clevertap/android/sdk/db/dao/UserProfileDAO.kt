package com.clevertap.android.sdk.db.dao

import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface UserProfileDAO {
    @WorkerThread
    fun storeUserProfile(accountId: String, deviceId: String, profile: JSONObject): Long
    
    @WorkerThread
    fun fetchUserProfilesByAccountId(accountId: String): Map<String, JSONObject>
    
    @WorkerThread
    fun fetchUserProfile(accountId: String, deviceId: String): JSONObject?
}
