package com.clevertap.android.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class CleverTapInstanceConfigTest {

    val configWithEncryption = CleverTapFixtures.provideCleverTapInstanceConfig()
    val configWithoutEncryption = CleverTapFixtures.configWithoutEncryptionKey()

    @Test
    fun `should encrypt returns true if public encryption key is provided`() {
        assertTrue(configWithEncryption.shouldEncryptResponse())
        assertFalse(configWithoutEncryption.shouldEncryptResponse())
    }
}