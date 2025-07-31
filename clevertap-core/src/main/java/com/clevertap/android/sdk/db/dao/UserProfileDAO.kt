package com.clevertap.android.sdk.db.dao

import androidx.annotation.WorkerThread
import org.json.JSONObject

interface UserProfileDAO {
    @WorkerThread
    fun storeUserProfile(accountId: String, deviceId: String, profile: JSONObject): Long
    
    @WorkerThread
    fun fetchUserProfilesByAccountId(accountId: String): Map<String, JSONObject>
    
    @WorkerThread
    fun fetchUserProfile(accountId: String, deviceId: String): JSONObject?
    
    @WorkerThread
    fun removeUserProfiles(accountId: String)
}
