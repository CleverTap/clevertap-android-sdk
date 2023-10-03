package com.clevertap.android.sdk.inapp

import android.content.Context
import android.content.SharedPreferences
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONArray
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import kotlin.test.assertNotNull

class InAppStoreTest : BaseTestCase() {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var cryptHandler: CryptHandler

    @Mock
    private lateinit var sharedPreferences: SharedPreferences

    @Mock
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var inAppStore: InAppStore

    override fun setUp() {
        super.setUp()
        MockitoAnnotations.openMocks(this)

        // Mock the sharedPrefs method to return the mocked SharedPreferences instance
        inAppStore = InAppStore(context, cryptHandler, "accountId", "deviceId")

        // Mock the sharedPrefs method to return the SharedPreferences instance
        `when`(inAppStore.sharedPrefs()).thenReturn(sharedPreferences)

        // Mock the SharedPreferences.Editor
        `when`(sharedPreferences.edit()).thenReturn(editor)
    }

    @Test
    fun testSetMode_WhenModeIsClientSide_ShouldRemoveServerSideInApps() {
        // Mock the editor's remove method
        `when`(editor.remove(anyString())).thenReturn(editor)

        inAppStore.mode = InAppStore.CLIENT_SIDE_MODE

        // Verify that the expected methods were called when setting the mode to CLIENT_SIDE_MODE
        verify(editor).remove(Constants.PREFS_INAPP_KEY_SS)
        verify(editor, never()).remove(Constants.PREFS_INAPP_KEY_CS)
        verify(editor).apply()
    }

    @Test
    fun testSetMode_WhenModeIsServerSide_ShouldRemoveClientSideInApps() {
        // Mock the editor's remove method
        `when`(editor.remove(anyString())).thenReturn(editor)

        inAppStore.mode = InAppStore.SERVER_SIDE_MODE

        // Verify that the expected methods were called when setting the mode to SERVER_SIDE_MODE
        verify(editor).remove(Constants.PREFS_INAPP_KEY_CS)
        verify(editor, never()).remove(Constants.PREFS_INAPP_KEY_SS)
        verify(editor).apply()
    }

    @Test
    fun testSetMode_WhenModeIsNoMode_ShouldRemoveBothInApps() {
        // Mock the editor's remove method
        `when`(editor.remove(anyString())).thenReturn(editor)

        inAppStore.mode = InAppStore.NO_MODE

        // Verify that the expected methods were called when setting the mode to NO_MODE
        verify(editor).remove(Constants.PREFS_INAPP_KEY_SS)
        verify(editor).remove(Constants.PREFS_INAPP_KEY_CS)
        verify(sharedPreferences.edit(), times(2)).apply()
    }

    @Test
    fun testStoreClientSideInApps_WhenCalled_ShouldPutInAppDataInSharedPreferences() {
        val jsonArray = JSONArray()
        val encryptedString = "encryptedData"

        `when`(
            cryptHandler.encrypt(
                jsonArray.toString(), Constants.KEY_ENCRYPTION_INAPP_CS
            )
        ).thenReturn(encryptedString)

        // Mock the editor's putString method
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)

        inAppStore.storeClientSideInApps(jsonArray)

        // Verify that the expected methods were called when storing Client-side In-App messages
        verify(editor).putString(Constants.PREFS_INAPP_KEY_CS, encryptedString)
        verify(editor).apply()
    }

    @Test
    fun testStoreServerSideInApps_WhenCalled_ShouldPutInAppDataInSharedPreferences() {
        val jsonArray = JSONArray()
        val encryptedString = "encryptedData"

        `when`(
            cryptHandler.encrypt(
                jsonArray.toString(), Constants.KEY_ENCRYPTION_INAPP_SS
            )
        ).thenReturn(encryptedString)

        // Mock the editor's putString method
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)

        inAppStore.storeServerSideInApps(jsonArray)

        // Verify that the expected methods were called when storing Client-side In-App messages
        verify(editor).putString(Constants.PREFS_INAPP_KEY_SS, encryptedString)
        verify(editor).apply()
    }

    @Test
    fun testReadClientSideInApps_WhenDataIsAvailable_ShouldReturnDecryptedData() {
        val jsonArray = JSONArray()
        val encryptedString = "encryptedData"
        val decryptedString = "[]"

        `when`(sharedPreferences.getString(Constants.PREFS_INAPP_KEY_CS, null)).thenReturn(
            encryptedString
        )
        `when`(cryptHandler.decrypt(encryptedString, Constants.KEY_ENCRYPTION_INAPP_CS)).thenReturn(
            decryptedString
        )

        val result = inAppStore.readClientSideInApps()

        // Verify that the expected methods were called when reading Client-side In-App messages
        verify(sharedPreferences).getString(Constants.PREFS_INAPP_KEY_CS, null)
        verify(cryptHandler).decrypt(encryptedString, Constants.KEY_ENCRYPTION_INAPP_CS)

        // Verify the result
        assert(result == jsonArray)
    }

    @Test
    fun testReadServerSideInApps_WhenDataIsAvailable_ShouldReturnDecryptedData() {
        val jsonArray = JSONArray()
        val encryptedString = "encryptedData"
        val decryptedString = "[]"

        `when`(sharedPreferences.getString(Constants.PREFS_INAPP_KEY_SS, null)).thenReturn(
            encryptedString
        )
        `when`(cryptHandler.decrypt(encryptedString, Constants.KEY_ENCRYPTION_INAPP_SS)).thenReturn(
            decryptedString
        )

        val result = inAppStore.readServerSideInApps()

        // Verify that the expected methods were called when reading Client-side In-App messages
        verify(sharedPreferences).getString(Constants.PREFS_INAPP_KEY_SS, null)
        verify(cryptHandler).decrypt(encryptedString, Constants.KEY_ENCRYPTION_INAPP_SS)

        // Verify the result
        assert(result == jsonArray)
    }

    @Test
    fun testSharedPrefs_WhenContextIsNonNull_ShouldReturnNonNull() {
        // Mock StorageHelper.getPreferences to return sharedPreferences
        `when`(StorageHelper.getPreferences(context, inAppStore.prefName)).thenReturn(
            sharedPreferences
        )

        val result = inAppStore.sharedPrefs()

        // Assert that the result is not null
        assertNotNull(result)
    }
}