package com.clevertap.android.sdk.validation

import org.junit.Assert.*
import org.junit.Test

class ValidationConfigTest {

    @Test
    fun `Builder creates empty config by default`() {
        val config = ValidationConfig.Builder().build()

        assertNull(config.maxKeyLength)
        assertNull(config.maxValueLength)
        assertNull(config.maxDepth)
        assertNull(config.maxArrayKeyCount)
        assertNull(config.maxObjectKeyCount)
        assertNull(config.maxArrayLength)
        assertNull(config.maxKVPairCount)
        assertNull(config.keyCharsNotAllowed)
        assertNull(config.valueCharsNotAllowed)
        assertNull(config.maxEventNameLength)
        assertNull(config.eventNameCharsNotAllowed)
        assertNull(config.restrictedEventNames)
        assertNull(config.restrictedMultiValueFields)
        assertNull(config.maxChargedEventItemsCount)
        assertNull(config.discardedEventNames)
    }

    @Test
    fun `Builder addKeySizeValidation sets max key length`() {
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(50)
            .build()

        assertEquals(50, config.maxKeyLength)
    }

    @Test
    fun `Builder addValueSizeValidation sets max value length`() {
        val config = ValidationConfig.Builder()
            .addValueLengthValidation(100)
            .build()

        assertEquals(100, config.maxValueLength)
    }

    @Test
    fun `Builder addDepthValidation sets max depth`() {
        val config = ValidationConfig.Builder()
            .addDepthValidation(5)
            .build()

        assertEquals(5, config.maxDepth)
    }

    @Test
    fun `Builder addArrayKeyCountValidation sets max array key count`() {
        val config = ValidationConfig.Builder()
            .addArrayKeyCountValidation(10)
            .build()

        assertEquals(10, config.maxArrayKeyCount)
    }

    @Test
    fun `Builder addObjectKeyCountValidation sets max object key count`() {
        val config = ValidationConfig.Builder()
            .addObjectKeyCountValidation(15)
            .build()

        assertEquals(15, config.maxObjectKeyCount)
    }

    @Test
    fun `Builder addArrayLengthValidation sets max array length`() {
        val config = ValidationConfig.Builder()
            .addArrayLengthValidation(200)
            .build()

        assertEquals(200, config.maxArrayLength)
    }

    @Test
    fun `Builder addKVPairCountValidation sets max KV pair count`() {
        val config = ValidationConfig.Builder()
            .addKVPairCountValidation(50)
            .build()

        assertEquals(50, config.maxKVPairCount)
    }

    @Test
    fun `Builder addKeyCharacterValidation sets disallowed key chars`() {
        val disallowed = setOf('!', '@', '#')
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(disallowed)
            .build()

        assertEquals(disallowed, config.keyCharsNotAllowed)
    }

    @Test
    fun `Builder addValueCharacterValidation sets disallowed value chars`() {
        val disallowed = setOf('$', '%', '&')
        val config = ValidationConfig.Builder()
            .addValueCharacterValidation(disallowed)
            .build()

        assertEquals(disallowed, config.valueCharsNotAllowed)
    }

    @Test
    fun `Builder addEventNameLengthValidation sets max event name length`() {
        val config = ValidationConfig.Builder()
            .addEventNameLengthValidation(75)
            .build()

        assertEquals(75, config.maxEventNameLength)
    }

    @Test
    fun `Builder addEventNameCharacterValidation sets disallowed event name chars`() {
        val disallowed = setOf('.', ':')
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(disallowed)
            .build()

        assertEquals(disallowed, config.eventNameCharsNotAllowed)
    }

    @Test
    fun `Builder setRestrictedEventNames sets restricted event names`() {
        val restricted = setOf("Event1", "Event2", "Event3")
        val config = ValidationConfig.Builder()
            .setRestrictedEventNames(restricted)
            .build()

        assertEquals(restricted, config.restrictedEventNames)
    }

    @Test
    fun `Builder setRestrictedMultiValueFields sets restricted multi-value fields`() {
        val restricted = setOf("name", "email", "phone")
        val config = ValidationConfig.Builder()
            .setRestrictedMultiValueFields(restricted)
            .build()

        assertEquals(restricted, config.restrictedMultiValueFields)
    }

    @Test
    fun `Builder addChargedEventItemsCountValidation sets max charged event items count`() {
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(25)
            .build()

        assertEquals(25, config.maxChargedEventItemsCount)
    }

    @Test
    fun `Builder setDiscardedEventNames sets discarded event names`() {
        val discarded = setOf("Discarded1", "Discarded2")
        val config = ValidationConfig.Builder()
            .setDiscardedEventNames(discarded)
            .build()

        assertEquals(discarded, config.discardedEventNames)
    }

    @Test
    fun `Builder setDeviceCountryCodeProvider sets country code provider`() {
        val provider: () -> String? = { "US" }
        val config = ValidationConfig.Builder()
            .setDeviceCountryCodeProvider(provider)
            .build()

        assertEquals("US", config.deviceCountryCodeProvider())
    }

    @Test
    fun `Builder without country code provider returns null`() {
        val config = ValidationConfig.Builder().build()

        assertNull(config.deviceCountryCodeProvider())
    }

    @Test
    fun `Builder chains multiple validations fluently`() {
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(50)
            .addValueLengthValidation(100)
            .addDepthValidation(5)
            .addArrayKeyCountValidation(10)
            .addObjectKeyCountValidation(15)
            .build()

        assertEquals(50, config.maxKeyLength)
        assertEquals(100, config.maxValueLength)
        assertEquals(5, config.maxDepth)
        assertEquals(10, config.maxArrayKeyCount)
        assertEquals(15, config.maxObjectKeyCount)
    }

    @Test
    fun `Builder allows overriding values`() {
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(50)
            .addKeyLengthValidation(100) // Override
            .build()

        assertEquals(100, config.maxKeyLength)
    }

    @Test
    fun `Builder from copies all values from existing config`() {
        val original = ValidationConfig.Builder()
            .addKeyLengthValidation(50)
            .addValueLengthValidation(100)
            .addDepthValidation(5)
            .addArrayKeyCountValidation(10)
            .addObjectKeyCountValidation(15)
            .addArrayLengthValidation(200)
            .addKVPairCountValidation(50)
            .addKeyCharacterValidation(setOf('!', '@'))
            .addValueCharacterValidation(setOf('#', '$'))
            .addEventNameLengthValidation(75)
            .addEventNameCharacterValidation(setOf('.', ':'))
            .setRestrictedEventNames(setOf("Event1"))
            .setRestrictedMultiValueFields(setOf("name"))
            .addChargedEventItemsCountValidation(25)
            .setDiscardedEventNames(setOf("Discarded1"))
            .setDeviceCountryCodeProvider { "US" }
            .build()

        val copy = ValidationConfig.Builder()
            .from(original)
            .build()

        assertEquals(original.maxKeyLength, copy.maxKeyLength)
        assertEquals(original.maxValueLength, copy.maxValueLength)
        assertEquals(original.maxDepth, copy.maxDepth)
        assertEquals(original.maxArrayKeyCount, copy.maxArrayKeyCount)
        assertEquals(original.maxObjectKeyCount, copy.maxObjectKeyCount)
        assertEquals(original.maxArrayLength, copy.maxArrayLength)
        assertEquals(original.maxKVPairCount, copy.maxKVPairCount)
        assertEquals(original.keyCharsNotAllowed, copy.keyCharsNotAllowed)
        assertEquals(original.valueCharsNotAllowed, copy.valueCharsNotAllowed)
        assertEquals(original.maxEventNameLength, copy.maxEventNameLength)
        assertEquals(original.eventNameCharsNotAllowed, copy.eventNameCharsNotAllowed)
        assertEquals(original.restrictedEventNames, copy.restrictedEventNames)
        assertEquals(original.restrictedMultiValueFields, copy.restrictedMultiValueFields)
        assertEquals(original.maxChargedEventItemsCount, copy.maxChargedEventItemsCount)
        assertEquals(original.discardedEventNames, copy.discardedEventNames)
        assertEquals("US", copy.deviceCountryCodeProvider())
    }

    @Test
    fun `Builder from allows modifying copied config`() {
        val original = ValidationConfig.Builder()
            .addKeyLengthValidation(50)
            .addValueLengthValidation(100)
            .build()

        val modified = ValidationConfig.Builder()
            .from(original)
            .addKeyLengthValidation(75) // Override
            .addDepthValidation(5) // Add new
            .build()

        assertEquals(75, modified.maxKeyLength)
        assertEquals(100, modified.maxValueLength)
        assertEquals(5, modified.maxDepth)
        // Original unchanged
        assertEquals(50, original.maxKeyLength)
        assertEquals(100, original.maxValueLength)
        assertNull(original.maxDepth)
    }

    @Test
    fun `updateDiscardedEventNames updates discarded names at runtime`() {
        val config = ValidationConfig.Builder()
            .setDiscardedEventNames(setOf("Initial1", "Initial2"))
            .build()

        assertEquals(setOf("Initial1", "Initial2"), config.discardedEventNames)

        config.updateDiscardedEventNames(setOf("Updated1", "Updated2", "Updated3"))

        assertEquals(setOf("Updated1", "Updated2", "Updated3"), config.discardedEventNames)
    }

    @Test
    fun `updateDiscardedEventNames can set to null`() {
        val config = ValidationConfig.Builder()
            .setDiscardedEventNames(setOf("Event1", "Event2"))
            .build()

        assertNotNull(config.discardedEventNames)

        config.updateDiscardedEventNames(null)

        assertNull(config.discardedEventNames)
    }

    @Test
    fun `updateDiscardedEventNames can be called multiple times`() {
        val config = ValidationConfig.Builder().build()

        config.updateDiscardedEventNames(setOf("First"))
        assertEquals(setOf("First"), config.discardedEventNames)

        config.updateDiscardedEventNames(setOf("Second"))
        assertEquals(setOf("Second"), config.discardedEventNames)

        config.updateDiscardedEventNames(setOf("Third", "Fourth"))
        assertEquals(setOf("Third", "Fourth"), config.discardedEventNames)
    }

    @Test
    fun `default builder creates config with default values`() {
        val config = ValidationConfig.default().build()

        assertNotNull(config.maxKeyLength)
        assertNotNull(config.maxValueLength)
        assertNotNull(config.maxDepth)
        assertNotNull(config.maxArrayKeyCount)
        assertNotNull(config.maxObjectKeyCount)
        assertNotNull(config.maxArrayLength)
        assertNotNull(config.maxKVPairCount)
        assertNotNull(config.keyCharsNotAllowed)
        assertNotNull(config.valueCharsNotAllowed)
        assertNotNull(config.maxEventNameLength)
        assertNotNull(config.eventNameCharsNotAllowed)
        assertNotNull(config.restrictedEventNames)
        assertNotNull(config.restrictedMultiValueFields)
        assertNotNull(config.maxChargedEventItemsCount)
    }

    @Test
    fun `default builder uses expected default values`() {
        val config = ValidationConfig.default().build()

        assertEquals(3, config.maxDepth)
        assertEquals(5, config.maxArrayKeyCount)
        assertEquals(5, config.maxObjectKeyCount)
        assertEquals(100, config.maxArrayLength)
        assertEquals(100, config.maxKVPairCount)
        assertEquals(50, config.maxChargedEventItemsCount)
    }

    @Test
    fun `default builder includes expected disallowed characters`() {
        val config = ValidationConfig.default().build()

        assertTrue(config.keyCharsNotAllowed!!.contains(':'))
        assertTrue(config.keyCharsNotAllowed.contains('$'))
        assertTrue(config.valueCharsNotAllowed!!.contains('\''))
        assertTrue(config.valueCharsNotAllowed.contains('"'))
        assertTrue(config.eventNameCharsNotAllowed!!.contains('.'))
        assertTrue(config.eventNameCharsNotAllowed.contains(':'))
    }

    @Test
    fun `default builder includes restricted event names`() {
        val config = ValidationConfig.default().build()

        assertNotNull(config.restrictedEventNames)
        assertTrue(config.restrictedEventNames!!.contains("Stayed"))
        assertTrue(config.restrictedEventNames.contains("App Launched"))
        assertTrue(config.restrictedEventNames.contains("Notification Clicked"))
    }

    @Test
    fun `default builder includes restricted multi-value fields`() {
        val config = ValidationConfig.default().build()

        assertNotNull(config.restrictedMultiValueFields)
        assertTrue(config.restrictedMultiValueFields!!.contains("name"))
        assertTrue(config.restrictedMultiValueFields.contains("email"))
        assertTrue(config.restrictedMultiValueFields.contains("phone"))
    }

    @Test
    fun `default builder with country code provider sets provider`() {
        val provider: () -> String? = { "IN" }
        val config = ValidationConfig.default(provider).build()

        assertEquals("IN", config.deviceCountryCodeProvider())
    }

    @Test
    fun `default builder without country code provider returns null`() {
        val config = ValidationConfig.default().build()

        assertNull(config.deviceCountryCodeProvider())
    }

    @Test
    fun `default builder can be modified with additional validations`() {
        val config = ValidationConfig.default()
            .addKeyLengthValidation(200) // Override default
            .setDiscardedEventNames(setOf("Custom1", "Custom2")) // Add new
            .build()

        assertEquals(200, config.maxKeyLength)
        assertEquals(setOf("Custom1", "Custom2"), config.discardedEventNames)
        // Other defaults still present
        assertEquals(3, config.maxDepth)
        assertNotNull(config.restrictedEventNames)
    }

    @Test
    fun `Builder handles empty sets`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(emptySet())
            .addValueCharacterValidation(emptySet())
            .setRestrictedEventNames(emptySet())
            .setDiscardedEventNames(emptySet())
            .build()

        assertEquals(emptySet<Char>(), config.keyCharsNotAllowed)
        assertEquals(emptySet<Char>(), config.valueCharsNotAllowed)
        assertEquals(emptySet<String>(), config.restrictedEventNames)
        assertEquals(emptySet<String>(), config.discardedEventNames)
    }

    @Test
    fun `Builder handles large validation limits`() {
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(10000)
            .addValueLengthValidation(50000)
            .addArrayLengthValidation(10000)
            .build()

        assertEquals(10000, config.maxKeyLength)
        assertEquals(50000, config.maxValueLength)
        assertEquals(10000, config.maxArrayLength)
    }

    @Test
    fun `Builder handles zero validation limits`() {
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(0)
            .addDepthValidation(0)
            .addArrayLengthValidation(0)
            .build()

        assertEquals(0, config.maxKeyLength)
        assertEquals(0, config.maxDepth)
        assertEquals(0, config.maxArrayLength)
    }

    @Test
    fun `Builder with mixed character sets for different validations`() {
        val keyChars = setOf(':', '$')
        val valueChars = setOf('\'', '"')
        val eventNameChars = setOf('.', ':')

        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(keyChars)
            .addValueCharacterValidation(valueChars)
            .addEventNameCharacterValidation(eventNameChars)
            .build()

        assertEquals(keyChars, config.keyCharsNotAllowed)
        assertEquals(valueChars, config.valueCharsNotAllowed)
        assertEquals(eventNameChars, config.eventNameCharsNotAllowed)
        // Ensure they are independent
        assertNotEquals(config.keyCharsNotAllowed, config.valueCharsNotAllowed)
        assertNotEquals(config.valueCharsNotAllowed, config.eventNameCharsNotAllowed)
    }

    @Test
    fun `Country code provider can return different values`() {
        var country = "US"
        val provider: () -> String? = { country }
        val config = ValidationConfig.Builder()
            .setDeviceCountryCodeProvider(provider)
            .build()

        assertEquals("US", config.deviceCountryCodeProvider())

        country = "IN"
        assertEquals("IN", config.deviceCountryCodeProvider())

        country = "UK"
        assertEquals("UK", config.deviceCountryCodeProvider())
    }

    @Test
    fun `Country code provider can return null`() {
        val provider: () -> String? = { null }
        val config = ValidationConfig.Builder()
            .setDeviceCountryCodeProvider(provider)
            .build()

        assertNull(config.deviceCountryCodeProvider())
    }

    @Test
    fun `DEFAULT_RESTRICTED_EVENT_NAMES contains expected values`() {
        val restricted = ValidationConfig.DEFAULT_RESTRICTED_EVENT_NAMES

        assertTrue(restricted.contains("Stayed"))
        assertTrue(restricted.contains("Notification Clicked"))
        assertTrue(restricted.contains("Notification Viewed"))
        assertTrue(restricted.contains("UTM Visited"))
        assertTrue(restricted.contains("App Launched"))
        assertTrue(restricted.contains("App Uninstalled"))
        assertTrue(restricted.contains("wzrk_d"))
    }

    @Test
    fun `DEFAULT_RESTRICTED_MULTI_VALUE_FIELDS contains expected values`() {
        val restricted = ValidationConfig.DEFAULT_RESTRICTED_MULTI_VALUE_FIELDS

        assertTrue(restricted.contains("name"))
        assertTrue(restricted.contains("email"))
        assertTrue(restricted.contains("education"))
        assertTrue(restricted.contains("married"))
        assertTrue(restricted.contains("dob"))
        assertTrue(restricted.contains("gender"))
        assertTrue(restricted.contains("phone"))
        assertTrue(restricted.contains("age"))
        assertTrue(restricted.contains("fbid"))
        assertTrue(restricted.contains("identity"))
    }

    @Test
    fun `Builder can build multiple independent configs`() {
        val builder = ValidationConfig.Builder()
            .addKeyLengthValidation(50)

        val config1 = builder.build()
        val config2 = builder.addValueLengthValidation(100).build()

        assertEquals(50, config1.maxKeyLength)
        assertNull(config1.maxValueLength)

        assertEquals(50, config2.maxKeyLength)
        assertEquals(100, config2.maxValueLength)
    }

    @Test
    fun `Config updateDiscardedEventNames does not affect builder`() {
        val builder = ValidationConfig.Builder()
            .setDiscardedEventNames(setOf("Original"))

        val config = builder.build()
        config.updateDiscardedEventNames(setOf("Modified"))

        // Build new config from same builder
        val newConfig = builder.build()

        assertEquals(setOf("Modified"), config.discardedEventNames)
        assertEquals(setOf("Original"), newConfig.discardedEventNames)
    }
}
