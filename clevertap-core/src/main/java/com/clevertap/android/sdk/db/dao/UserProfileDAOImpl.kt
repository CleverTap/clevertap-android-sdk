package com.clevertap.android.sdk.db.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.db.Column
import com.clevertap.android.sdk.db.DBAdapter.Companion.DB_OUT_OF_MEMORY_ERROR
import com.clevertap.android.sdk.db.DBAdapter.Companion.DB_UPDATE_ERROR
import com.clevertap.android.sdk.db.DBAdapter.Companion.NOT_ENOUGH_SPACE_LOG
import com.clevertap.android.sdk.db.DBEncryptionHandler
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.sdk.db.Table.USER_PROFILES
import org.json.JSONObject

internal class UserProfileDAOImpl(
    private val dbHelper: DatabaseHelper,
    private val logger: ILogger,
    private val dbEncryptionHandler: DBEncryptionHandler,
) : UserProfileDAO {

    @WorkerThread
    override fun storeUserProfile(accountId: String, deviceId: String, profile: JSONObject): Long {
        if (!dbHelper.belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return DB_OUT_OF_MEMORY_ERROR
        }
        
        val tableName = USER_PROFILES.tableName
        logger.verbose("Inserting or updating userProfile for accountID = $accountId + deviceID = $deviceId")
        
        val cv = ContentValues().apply {
            val encryptedProfile = dbEncryptionHandler.wrapDbData(profile.toString())
            put(Column.DATA, encryptedProfile)
            put(Column.ID, accountId)
            put(Column.DEVICE_ID, deviceId)
        }

        return try {
            dbHelper.writableDatabase.insertWithOnConflict(
                tableName, null, cv, SQLiteDatabase.CONFLICT_REPLACE
            )
        } catch (e: SQLiteException) {
            logger.verbose("Error adding data to table $tableName. Recreating DB", e)
            dbHelper.deleteDatabase()
            DB_UPDATE_ERROR
        }
    }

    @WorkerThread
    override fun fetchUserProfilesByAccountId(accountId: String): Map<String, JSONObject> {
        val profiles = mutableMapOf<String, JSONObject>()
        val tName = USER_PROFILES.tableName

        try {
            dbHelper.readableDatabase.query(
                tName,
                null,
                "${Column.ID} = ?",
                arrayOf(accountId),
                null,
                null,
                null
            )?.use { cursor ->
                val dataIndex = cursor.getColumnIndex(Column.DATA)
                val deviceIdIndex = cursor.getColumnIndex(Column.DEVICE_ID)
                
                while (cursor.moveToNext()) {
                    val profileString = cursor.getString(dataIndex)
                    val deviceIdString = cursor.getString(deviceIdIndex)

                    val decryptedProfile = dbEncryptionHandler.unwrapDbData(profileString)
                    if (decryptedProfile != null) {
                        val jsonObject = JSONObject(decryptedProfile)
                        profiles[deviceIdString] = jsonObject
                    } else {
                        logger.verbose("Error decrypting JSON for profile")
                    }
                }
            }
        } catch (e: SQLiteException) {
            logger.verbose("Could not fetch records out of database $tName.", e)
        }

        return profiles
    }

    @WorkerThread
    override fun fetchUserProfile(accountId: String, deviceId: String): JSONObject? {
        val tName = USER_PROFILES.tableName

        var op: JSONObject? = null
        try {
            dbHelper.readableDatabase.query(
                tName, null,
                "${Column.ID} = ? AND ${Column.DEVICE_ID} = ?",
                arrayOf(accountId, deviceId),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dataIndex = cursor.getColumnIndex(Column.DATA)
                    if (dataIndex >= 0) {
                        val profileString = cursor.getString(dataIndex)
                        val decryptedProfile = dbEncryptionHandler.unwrapDbData(profileString)
                        if (decryptedProfile != null) {
                            op = JSONObject(decryptedProfile)
                        } else {
                            logger.verbose("Failed profile decryption")
                        }
                    }
                } else {
                    logger.verbose("There was no profile found in DB")
                }
            }
        } catch (e: SQLiteException) {
            logger.verbose("Could not fetch records out of database $tName.", e)
        }
        return op
    }
}
