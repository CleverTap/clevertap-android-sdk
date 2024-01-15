package com.clevertap.android.sdk.store.preference

import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.*
import org.junit.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CTPreferenceTest : BaseTestCase() {

    private lateinit var ctPreference: CTPreference
    override fun setUp() {
        super.setUp()
        ctPreference = spyk(CTPreference(application, "ct_pref"))
    }

    @Test
    fun `readString returns default when sharedPrefs() returns null`() {
        // Arrange
        every { ctPreference.sharedPrefs() } returns null
        // Act
        val result = ctPreference.readString("key", "default")

        // Assert
        assertEquals("default", result)
    }

    @Test
    fun `readString returns value from sharedPrefs`() {
        // Arrange
        ctPreference.writeString("key", "storedValue")

        // Act
        val result = ctPreference.readString("key", "default")

        // Assert
        assertEquals("storedValue", result)
    }

    @Test
    fun `readString returns default value from sharedPrefs when key not present`() {

        // Act
        val result = ctPreference.readString("key", "default")

        // Assert
        assertEquals("default", result)
    }

    @Test
    fun `readBoolean returns default when sharedPrefs() returns null`() {
        // Arrange
        every { ctPreference.sharedPrefs() } returns null

        // Act
        val result = ctPreference.readBoolean("key", true)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `readBoolean returns value from sharedPrefs`() {
        // Arrange
        ctPreference.writeBoolean("key", true)

        // Act
        val result = ctPreference.readBoolean("key", false)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `readBoolean returns default value from sharedPrefs when key not present`() {
        // Act
        val result = ctPreference.readBoolean("key", true)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `readInt returns default when sharedPrefs() returns null`() {
        every { ctPreference.sharedPrefs() } returns null
        val result = ctPreference.readInt("key", 42)
        assertEquals(42, result)
    }

    @Test
    fun `readInt returns value from sharedPrefs`() {
        ctPreference.writeInt("key", 42)
        val result = ctPreference.readInt("key", 0)
        assertEquals(42, result)
    }

    @Test
    fun `readInt returns default value from sharedPrefs when key not present`() {
        val result = ctPreference.readInt("key", 42)
        assertEquals(42, result)
    }

    @Test
    fun `readLong returns default when sharedPrefs() returns null`() {
        every { ctPreference.sharedPrefs() } returns null
        val result = ctPreference.readLong("key", 42L)
        assertEquals(42L, result)
    }

    @Test
    fun `readLong returns value from sharedPrefs`() {
        ctPreference.writeLong("key", 42L)
        val result = ctPreference.readLong("key", 0L)
        assertEquals(42L, result)
    }

    @Test
    fun `readLong returns default value from sharedPrefs when key not present`() {
        val result = ctPreference.readLong("key", 42L)
        assertEquals(42L, result)
    }

    @Test
    fun `readFloat returns default when sharedPrefs() returns null`() {
        every { ctPreference.sharedPrefs() } returns null
        val result = ctPreference.readFloat("key", 42.0f)
        assertEquals(42.0f, result, 0.0f)
    }

    @Test
    fun `readFloat returns value from sharedPrefs`() {
        ctPreference.writeFloat("key", 42.0f)
        val result = ctPreference.readFloat("key", 0.0f)
        assertEquals(42.0f, result, 0.0f)
    }

    @Test
    fun `readFloat returns default value from sharedPrefs when key not present`() {
        val result = ctPreference.readFloat("key", 42.0f)
        assertEquals(42.0f, result, 0.0f)
    }

    @Test
    fun `readStringSet returns default when sharedPrefs() returns null`() {
        every { ctPreference.sharedPrefs() } returns null
        val result = ctPreference.readStringSet("key", setOf("default"))
        assertEquals(setOf("default"), result)
    }

    @Test
    fun `readStringSet returns value from sharedPrefs`() {
        ctPreference.writeStringSet("key", setOf("value1", "value2"))
        val result = ctPreference.readStringSet("key", setOf("default"))
        assertEquals(setOf("value1", "value2"), result)
    }

    @Test
    fun `readStringSet returns default value from sharedPrefs when key not present`() {
        val result = ctPreference.readStringSet("key", setOf("default"))
        assertEquals(setOf("default"), result)
    }

    @Test
    fun `readAll returns an empty map when sharedPrefs() returns null`() {
        every { ctPreference.sharedPrefs() } returns null
        val result = ctPreference.readAll()
        assertTrue(result?.isEmpty() ?: false)
    }

    @Test
    fun `readAll returns all entries from sharedPrefs`() {
        // Arrange
        ctPreference.writeString("stringKey", "stringValue")
        ctPreference.writeBoolean("booleanKey", true)
        ctPreference.writeInt("intKey", 42)
        ctPreference.writeLong("longKey", 123L)
        ctPreference.writeFloat("floatKey", 3.14f)
        ctPreference.writeStringSet("setKey", setOf("value1", "value2"))

        // Act
        val result = ctPreference.readAll()

        // Assert
        assertEquals(
            mapOf(
                "stringKey" to "stringValue",
                "booleanKey" to true,
                "intKey" to 42,
                "longKey" to 123L,
                "floatKey" to 3.14f,
                "setKey" to setOf("value1", "value2")
            ), result
        )
    }

    @Test
    fun `writeString does not write when sharedPrefs() returns null`() {
        // Arrange
        every { ctPreference.sharedPrefs() } returns null
        // Act
        ctPreference.writeString("key", "value")

        // Assert
        every { ctPreference.sharedPrefs() } answers { callOriginal() }
        assertEquals("default", ctPreference.readString("key", "default"))
    }

    @Test
    fun `writeStringImmediate does not write when sharedPrefs() returns null`() {
        // Arrange
        every { ctPreference.sharedPrefs() } returns null
        // Act
        ctPreference.writeStringImmediate("key", "value")
        // Assert
        every { ctPreference.sharedPrefs() } answers { callOriginal() }
        assertEquals("default", ctPreference.readString("key", "default"))
    }

    @Test
    fun `writeStringImmediate writes when sharedPrefs() not null`() {
        // Act
        ctPreference.writeStringImmediate("key", "value")
        // Assert
        assertEquals("value", ctPreference.readString("key", "default"))
    }

    @Test
    fun `writeBoolean does not write when sharedPrefs is null`() {
        every { ctPreference.sharedPrefs() } returns null
        ctPreference.writeBoolean("key", true)
        every { ctPreference.sharedPrefs() } answers { callOriginal() }
        assertFalse(ctPreference.readBoolean("key", false))
    }

    @Test
    fun `writeBooleanImmediate does not write when sharedPrefs is null`() {
        every { ctPreference.sharedPrefs() } returns null
        ctPreference.writeBooleanImmediate("key", true)
        every { ctPreference.sharedPrefs() } answers { callOriginal() }
        assertFalse(ctPreference.readBoolean("key", false))
    }

    @Test
    fun `writeBooleanImmediate writes when sharedPrefs is not null`() {
        ctPreference.writeBooleanImmediate("key", true)
        assertTrue(ctPreference.readBoolean("key", false))
    }

    @Test
    fun `writeInt does not write when sharedPrefs is null`() {
        every { ctPreference.sharedPrefs() } returns null
        ctPreference.writeInt("key", 42)
        every { ctPreference.sharedPrefs() } answers { callOriginal() }
        assertEquals(0, ctPreference.readInt("key", 0))
    }

    @Test
    fun `writeIntImmediate does not write when sharedPrefs is null`() {
        every { ctPreference.sharedPrefs() } returns null
        ctPreference.writeIntImmediate("key", 42)
        every { ctPreference.sharedPrefs() } answers { callOriginal() }
        assertEquals(0, ctPreference.readInt("key", 0))
    }

    @Test
    fun `writeIntImmediate writes when sharedPrefs is not null`() {
        ctPreference.writeIntImmediate("key", 42)
        assertEquals(42, ctPreference.readInt("key", 0))
    }

    @Test
    fun `writeLong does not write when sharedPrefs is null`() {
        every { ctPreference.sharedPrefs() } returns null
        ctPreference.writeLong("key", 123L)
        every { ctPreference.sharedPrefs() } answers { callOriginal() }
        assertEquals(0L, ctPreference.readLong("key", 0L))
    }

    @Test
    fun `writeLongImmediate does not write when sharedPrefs is null`() {
        every { ctPreference.sharedPrefs() } returns null
        ctPreference.writeLongImmediate("key", 123L)
        every { ctPreference.sharedPrefs() } answers { callOriginal() }
        assertEquals(0L, ctPreference.readLong("key", 0L))
    }

    @Test
    fun `writeLongImmediate writes when sharedPrefs is not null`() {
        ctPreference.writeLongImmediate("key", 123L)
        assertEquals(123L, ctPreference.readLong("key", 0L))
    }

    @Test
    fun `writeFloat does not write when sharedPrefs is null`() {
        every { ctPreference.sharedPrefs() } returns null
        ctPreference.writeFloat("key", 3.14f)
        every { ctPreference.sharedPrefs() } answers { callOriginal() }
        assertEquals(0.0f, ctPreference.readFloat("key", 0.0f), 0.0f)
    }

    @Test
    fun `writeFloatImmediate does not write when sharedPrefs is null`() {
        every { ctPreference.sharedPrefs() } returns null
        ctPreference.writeFloatImmediate("key", 3.14f)
        every { ctPreference.sharedPrefs() } answers { callOriginal() }
        assertEquals(0.0f, ctPreference.readFloat("key", 0.0f), 0.0f)
    }

    @Test
    fun `writeFloatImmediate writes when sharedPrefs is not null`() {
        ctPreference.writeFloatImmediate("key", 3.14f)
        assertEquals(3.14f, ctPreference.readFloat("key", 0.0f), 0.0f)
    }

    @Test
    fun `writeStringSet does not write when sharedPrefs is null`() {
        every { ctPreference.sharedPrefs() } returns null
        ctPreference.writeStringSet("key", setOf("value1", "value2"))
        every { ctPreference.sharedPrefs() } answers { callOriginal() }
        assertEquals(setOf(), ctPreference.readStringSet("key", setOf()))
    }

    @Test
    fun `writeStringSetImmediate does not write when sharedPrefs is null`() {
        every { ctPreference.sharedPrefs() } returns null
        ctPreference.writeStringSetImmediate("key", setOf("value1", "value2"))
        every { ctPreference.sharedPrefs() } answers { callOriginal() }
        assertEquals(setOf(), ctPreference.readStringSet("key", setOf()))
    }

    @Test
    fun `writeStringSetImmediate writes when sharedPrefs is not null`() {
        ctPreference.writeStringSetImmediate("key", setOf("value1", "value2"))
        assertEquals(setOf("value1", "value2"), ctPreference.readStringSet("key", setOf()))
    }

    @Test
    fun `writeMap does not write when sharedPrefs is null`() {
        every { ctPreference.sharedPrefs() } returns null
        ctPreference.writeMap("mapKey", mapOf("subKey" to "subValue"))
        every { ctPreference.sharedPrefs() } answers { callOriginal() }
        assertEquals(mapOf<String, Any>(), ctPreference.readAll())
    }

    @Test
    fun `writeMapImmediate does not write when sharedPrefs is null`() {
        every { ctPreference.sharedPrefs() } returns null
        ctPreference.writeMapImmediate("mapKey", mapOf("subKey" to "subValue"))
        every { ctPreference.sharedPrefs() } answers { callOriginal() }
        assertEquals(mapOf<String, Any>(), ctPreference.readAll())
    }

    @Test
    fun `writeMapImmediate writes when sharedPrefs is not null`() {
        ctPreference.writeMapImmediate("mapKey", mapOf("subKey" to "subValue"))
        assertEquals(mapOf("subKey" to "subValue"), ctPreference.readAll())
    }

    @Test
    fun `writeMapImmediate writes only primitive types`() {
        ctPreference.writeMapImmediate(
            "mapKey", mapOf(
                "stringKey" to "stringValue",
                "booleanKey" to true,
                "intKey" to 42,
                "longKey" to 123L,
                "floatKey" to 3.14f,
                "setKey" to setOf("value1", "value2")
            )
        )
        assertEquals(
            mapOf(
                "stringKey" to "stringValue",
                "booleanKey" to true,
                "intKey" to 42,
                "longKey" to 123L,
                "floatKey" to 3.14f
            ), ctPreference.readAll()
        )
    }

    @Test
    fun `isEmpty returns true when sharedPrefs is null`() {
        every { ctPreference.sharedPrefs() } returns null
        assertTrue(ctPreference.isEmpty())
    }

    @Test
    fun `isEmpty returns true when sharedPrefs is empty`() {
        assertTrue(ctPreference.isEmpty())
    }

    @Test
    fun `isEmpty returns false when sharedPrefs is not empty`() {
        ctPreference.writeString("key", "value")
        assertFalse(ctPreference.isEmpty())
    }

    @Test
    fun `size returns 0 when sharedPrefs is null`() {
        every { ctPreference.sharedPrefs() } returns null
        assertEquals(0, ctPreference.size())
    }

    @Test
    fun `size returns 0 when sharedPrefs is empty`() {
        assertEquals(0, ctPreference.size())
    }

    @Test
    fun `size returns the correct size when sharedPrefs is not empty`() {
        ctPreference.writeString("key1", "value1")
        ctPreference.writeString("key2", "value2")
        assertEquals(2, ctPreference.size())
    }

    @Test
    fun `remove does not remove when sharedPrefs is null`() {
        ctPreference.writeStringImmediate("key", "value")
        every { ctPreference.sharedPrefs() } returns null
        ctPreference.remove("key")
        every { ctPreference.sharedPrefs() } answers { callOriginal() }
        assertEquals("value", ctPreference.readString("key", "default"))
    }

    @Test
    fun `remove removes the entry when sharedPrefs is not null`() {
        ctPreference.writeString("key", "value")
        ctPreference.remove("key")
        assertEquals("default", ctPreference.readString("key", "default"))
    }

    @Test
    fun `removeImmediate does not remove when sharedPrefs is null`() {
        ctPreference.writeString("key", "value")
        every { ctPreference.sharedPrefs() } returns null
        ctPreference.removeImmediate("key")
        every { ctPreference.sharedPrefs() } answers { callOriginal() }
        assertEquals("value", ctPreference.readString("key", "default"))
    }

    @Test
    fun `removeImmediate removes the entry when sharedPrefs is not null`() {
        ctPreference.writeString("key", "value")
        ctPreference.removeImmediate("key")
        assertEquals("default", ctPreference.readString("key", "default"))
    }

    @Test
    fun `changePreferenceName updates prefName`() {
        // Arrange
        val ctPreference1 = CTPreference(application, "test_prefs_1")
        ctPreference1.writeString("k1", "v1")

        // Act
        ctPreference1.changePreferenceName("new_test_prefs")
        ctPreference1.writeString("k2", "v2")

        // Assert
        assertEquals("default", ctPreference1.readString("k1", "default"))
        assertEquals("v2", ctPreference1.readString("k2", "default"))
    }
}