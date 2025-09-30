package com.clevertap.android.sdk.cryption

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EncryptionLevelTest {

    private val NONE = EncryptionLevel.NONE
    private val MEDIUM = EncryptionLevel.MEDIUM
    private val FULL_DATA = EncryptionLevel.FULL_DATA

    @Test
    fun `EncryptionLevel - check shouldEncrypt method to return correct value`() {
        assertTrue(MEDIUM.shouldEncrypt())
        assertTrue(FULL_DATA.shouldEncrypt())
        assertFalse(NONE.shouldEncrypt())
    }

    @Test
    fun `EncryptionLevel - check correct to int values`() {
        assertEquals(1,MEDIUM.intValue())
        assertEquals(2,FULL_DATA.intValue())
        assertEquals(0,NONE.intValue())
    }

    @Test
    fun `EncryptionLevel - check correct from int values`() {
        assertEquals(EncryptionLevel.fromInt(0), EncryptionLevel.NONE)
        assertEquals(EncryptionLevel.fromInt(1), EncryptionLevel.MEDIUM)
        assertEquals(EncryptionLevel.fromInt(2), EncryptionLevel.FULL_DATA)
    }
}