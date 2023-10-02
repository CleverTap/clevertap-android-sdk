package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class InAppStoreTest : BaseTestCase() {

    @Mock
    private lateinit var cryptHandler: CryptHandler

    private lateinit var inAppStore: InAppStore

    override fun setUp() {
        super.setUp()
        MockitoAnnotations.openMocks(this)

        // Mock the sharedPrefs method to return the mocked SharedPreferences instance
        inAppStore = InAppStore(appCtx, cryptHandler, "accountId", "deviceId")
    }

    @Test
    fun testSetModeClientSide() {
        //TODO
    }
}