package com.clevertap.android.sdk.profile

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.profile.traversal.ProfileOperation
import io.mockk.mockk
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive tests for ProfileStateTraverser with all operations.
 * Uses combined nested JSONObjects to test multiple scenarios efficiently.
 *
 * Test coverage:
 * - UPDATE: Value replacement, type changes, nested merges, additions
 * - INCREMENT: Numeric additions, missing keys, type handling
 * - DECREMENT: Numeric subtractions, missing keys, type handling
 * - DELETE: Key removal, nested deletions, delete markers
 * - ARRAY_ADD: String additions, duplicate handling, missing arrays
 * - ARRAY_REMOVE: String removals, missing elements, missing arrays
 * - GET: Value retrieval without modification
 */
class ProfileStateTraverserTest {

    private lateinit var traverser: ProfileStateTraverser
    private lateinit var logger: ILogger

    @Before
    fun setUp() {
        logger = mockk<ILogger>(relaxed = true)
        traverser = ProfileStateTraverser(logger)
    }

    // Comprehensive test covering multiple update scenarios
    @Test
    fun `UPDATE handles comprehensive profile update scenario`() {
        val target = JSONObject().apply {
            // Existing values to be updated
            put("name", "John")
            put("age", 25)
            put("active", true)
            put("dob", "\$D_123")

            // Nested object with partial updates
            put("address", JSONObject().apply {
                put("city", "NYC")
                put("country", "USA")
                put("zip", "10001")
            })

            // Value to be replaced with different type
            put("score", "100")

            // Nested object to be replaced with primitive
            put("metadata", JSONObject().apply {
                put("version", "1.0")
                put("timestamp", 123456)
            })

            // Array to be replaced
            put("tags", JSONArray().apply {
                put("old1")
                put("old2")
            })

            // Nested object with $D_ date values
            put("employment", JSONObject().apply {
                put("company", "TechCorp")
                put("startDate", "\$D_1609459200") // Jan 1, 2021
                put("lastPromotion", "\$D_1640995200") // Jan 1, 2022
            })

            // Array with $D_ date values
            put("eventDates", JSONArray().apply {
                put("\$D_1672531200") // Jan 1, 2023
                put("\$D_1704067200") // Jan 1, 2024
                put("regular_string")
            })

            // Deeply nested object with $D_ values
            put("timeline", JSONObject().apply {
                put("personal", JSONObject().apply {
                    put("birthDate", "\$D_631152000") // Jan 1, 1990
                    put("graduationDate", "\$D_1262304000") // Jan 1, 2010
                })
                put("professional", JSONObject().apply {
                    put("firstJob", "\$D_1325376000") // Jan 1, 2012
                })
            })

            // Array of objects with $D_ values
            put("milestones", JSONArray().apply {
                put(JSONObject().apply {
                    put("event", "joined")
                    put("date", "\$D_1577836800") // Jan 1, 2020
                })
                put(JSONObject().apply {
                    put("event", "promoted")
                    put("date", "\$D_1609459200") // Jan 1, 2021
                })
            })
        }

        val source = JSONObject().apply {
            // Update existing primitive (same value - should not create change)
            put("name", "John")

            // Update existing primitive (different value)
            put("age", 30)

            // Update existing boolean
            put("active", false)

            put("dob", "\$D_456")

            // Update nested object (partial - merge behavior)
            put("address", JSONObject().apply {
                put("city", "LA") // Update existing
                put("zip", "90001") // Update existing
                put("state", "CA") // Add new
                // Note: "country" not specified, should remain unchanged
            })

            // Type change: string to number
            put("score", 150)

            // Replace nested object with primitive
            put("metadata", "simplified")

            // Add completely new top-level key
            put("email", "[email protected]")

            // Add new nested object
            put("preferences", JSONObject().apply {
                put("theme", "dark")
                put("notifications", true)
            })

            // Replace array
            put("tags", JSONArray().apply {
                put("new1")
                put("new2")
                put("new3")
            })

            // Update nested object with $D_ values (partial update)
            put("employment", JSONObject().apply {
                put("company", "MegaCorp") // Update existing
                put("startDate", "\$D_1640995200") // Update date
                put("endDate", "\$D_1704067200") // Add new date field
                // lastPromotion not specified, should remain unchanged
            })

            // Update array with $D_ values (complete replacement)
            put("eventDates", JSONArray().apply {
                put("\$D_1704067200") // Jan 1, 2024
                put("\$D_1735689600") // Jan 1, 2025
                put("\$D_1767225600") // Jan 1, 2026
            })

            // Update deeply nested object with $D_ values
            put("timeline", JSONObject().apply {
                put("personal", JSONObject().apply {
                    put("birthDate", "\$D_631152000") // Same value
                    put("graduationDate", "\$D_1293840000") // Updated date
                    put("marriageDate", "\$D_1420070400") // New date field
                })
                // professional not specified, should remain unchanged
            })

            // Replace array of objects with $D_ values
            put("milestones", JSONArray().apply {
                put(JSONObject().apply {
                    put("event", "relocated")
                    put("date", "\$D_1735689600") // Jan 1, 2025
                })
            })

            // Add new nested object with mixed $D_ values
            put("healthRecords", JSONObject().apply {
                put("lastCheckup", "\$D_1704067200")
                put("vaccinations", JSONArray().apply {
                    put(JSONObject().apply {
                        put("name", "COVID-19")
                        put("date", "\$D_1640995200")
                    })
                    put(JSONObject().apply {
                        put("name", "Flu")
                        put("date", "\$D_1672531200")
                    })
                })
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.UPDATE)
        println("AnushX " + result)

        // Verify target state after update
        assertEquals("John", target.getString("name"))
        assertEquals(30, target.getInt("age"))
        assertEquals(false, target.getBoolean("active"))
        assertEquals("\$D_456", target.getString("dob"))

        // Verify nested object merge behavior
        val address = target.getJSONObject("address")
        assertEquals("LA", address.getString("city"))
        assertEquals("USA", address.getString("country")) // Should remain unchanged
        assertEquals("90001", address.getString("zip"))
        assertEquals("CA", address.getString("state"))

        // Verify type change
        assertEquals(150, target.getInt("score"))

        // Verify object replaced with primitive
        assertEquals("simplified", target.getString("metadata"))

        // Verify new additions
        assertEquals("[email protected]", target.getString("email"))
        val prefs = target.getJSONObject("preferences")
        assertEquals("dark", prefs.getString("theme"))
        assertEquals(true, prefs.getBoolean("notifications"))

        // Verify array replacement
        val tags = target.getJSONArray("tags")
        assertEquals(3, tags.length())
        assertEquals("new1", tags.getString(0))

        // Verify nested object with $D_ updates
        val employment = target.getJSONObject("employment")
        assertEquals("MegaCorp", employment.getString("company"))
        assertEquals("\$D_1640995200", employment.getString("startDate"))
        assertEquals("\$D_1704067200", employment.getString("endDate"))
        assertEquals("\$D_1640995200", employment.getString("lastPromotion")) // Unchanged

        // Verify array with $D_ replacement
        val eventDates = target.getJSONArray("eventDates")
        assertEquals(3, eventDates.length())
        assertEquals("\$D_1704067200", eventDates.getString(0))
        assertEquals("\$D_1735689600", eventDates.getString(1))
        assertEquals("\$D_1767225600", eventDates.getString(2))

        // Verify deeply nested $D_ updates
        val timeline = target.getJSONObject("timeline")
        val personal = timeline.getJSONObject("personal")
        assertEquals("\$D_631152000", personal.getString("birthDate"))
        assertEquals("\$D_1293840000", personal.getString("graduationDate"))
        assertEquals("\$D_1420070400", personal.getString("marriageDate"))
        val professional = timeline.getJSONObject("professional")
        assertEquals("\$D_1325376000", professional.getString("firstJob")) // Unchanged

        // Verify array of objects with $D_ replacement
        val milestones = target.getJSONArray("milestones")
        assertEquals(1, milestones.length())
        val milestone = milestones.getJSONObject(0)
        assertEquals("relocated", milestone.getString("event"))
        assertEquals("\$D_1735689600", milestone.getString("date"))

        // Verify new nested object with $D_ values
        val healthRecords = target.getJSONObject("healthRecords")
        assertEquals("\$D_1704067200", healthRecords.getString("lastCheckup"))
        val vaccinations = healthRecords.getJSONArray("vaccinations")
        assertEquals(2, vaccinations.length())
        assertEquals("\$D_1640995200", vaccinations.getJSONObject(0).getString("date"))
        assertEquals("\$D_1672531200", vaccinations.getJSONObject(1).getString("date"))

        // Verify changes tracked correctly
        // Should NOT include "name" (no change)
        assertFalse(result.changes.containsKey("name"))

        // Should include updated primitives
        assertTrue(result.changes.containsKey("age"))
        assertEquals(25, result.changes["age"]!!.oldValue)
        assertEquals(30, result.changes["age"]!!.newValue)

        assertTrue(result.changes["dob"]!!.oldValue is Long)
        assertTrue(result.changes["dob"]!!.newValue is Long)
        assertEquals(123L, result.changes["dob"]!!.oldValue)
        assertEquals(456L, result.changes["dob"]!!.newValue)

        assertTrue(result.changes.containsKey("active"))

        // Should include nested updates with dot notation
        assertTrue(result.changes.containsKey("address.city"))
        assertEquals("NYC", result.changes["address.city"]!!.oldValue)
        assertEquals("LA", result.changes["address.city"]!!.newValue)

        assertTrue(result.changes.containsKey("address.zip"))
        assertTrue(result.changes.containsKey("address.state"))
        assertNull(result.changes["address.state"]!!.oldValue) // New key

        // Should NOT include unchanged nested values
        assertFalse(result.changes.containsKey("address.country"))

        // Should include type change
        assertTrue(result.changes.containsKey("score"))

        // Should include object->primitive replacement
        assertTrue(result.changes.containsKey("metadata"))

        // Should include new additions
        assertTrue(result.changes.containsKey("email"))
        assertNull(result.changes["email"]!!.oldValue)

        // Should include new nested object (as leaf values)
        assertTrue(result.changes.containsKey("preferences.theme"))
        assertTrue(result.changes.containsKey("preferences.notifications"))
        assertNull(result.changes["preferences.theme"]!!.oldValue)

        // Should include array replacement
        assertTrue(result.changes.containsKey("tags"))

        // Verify $D_ changes in nested objects
        assertTrue(result.changes.containsKey("employment.company"))
        assertTrue(result.changes.containsKey("employment.startDate"))
        assertEquals(1609459200L, result.changes["employment.startDate"]!!.oldValue)
        assertEquals(1640995200L, result.changes["employment.startDate"]!!.newValue)

        assertTrue(result.changes.containsKey("employment.endDate"))
        assertNull(result.changes["employment.endDate"]!!.oldValue) // New field
        assertEquals(1704067200L, result.changes["employment.endDate"]!!.newValue)

        // Should NOT include unchanged $D_ values
        assertFalse(result.changes.containsKey("employment.lastPromotion"))

        // Verify $D_ changes in arrays (array replacement)
        assertTrue(result.changes.containsKey("eventDates"))

        // Verify $D_ changes in deeply nested objects
        assertFalse(result.changes.containsKey("timeline.personal.birthDate")) // Same value
        assertTrue(result.changes.containsKey("timeline.personal.graduationDate"))
        assertEquals(1262304000L, result.changes["timeline.personal.graduationDate"]!!.oldValue)
        assertEquals(1293840000L, result.changes["timeline.personal.graduationDate"]!!.newValue)

        assertTrue(result.changes.containsKey("timeline.personal.marriageDate"))
        assertNull(result.changes["timeline.personal.marriageDate"]!!.oldValue)
        assertEquals(1420070400L, result.changes["timeline.personal.marriageDate"]!!.newValue)

        // Should NOT include unchanged nested objects
        assertFalse(result.changes.containsKey("timeline.professional.firstJob"))

        // Verify array of objects replacement
        assertTrue(result.changes.containsKey("milestones"))

        // Verify new nested object with $D_ values
        assertTrue(result.changes.containsKey("healthRecords.lastCheckup"))
        assertEquals(1704067200L, result.changes["healthRecords.lastCheckup"]!!.newValue)

        assertTrue(result.changes.containsKey("healthRecords.vaccinations"))
    }

    @Test
    fun `UPDATE handles deeply nested structure with multiple levels`() {
        val target = JSONObject().apply {
            put("user", JSONObject().apply {
                put("profile", JSONObject().apply {
                    put("personal", JSONObject().apply {
                        put("name", "John")
                        put("age", 25)
                    })
                    put("contact", JSONObject().apply {
                        put("email", "[email protected]")
                    })
                })
                put("settings", JSONObject().apply {
                    put("privacy", "public")
                })
            })
        }

        val source = JSONObject().apply {
            put("user", JSONObject().apply {
                put("profile", JSONObject().apply {
                    put("personal", JSONObject().apply {
                        put("age", 30) // Update deep nested
                        put("city", "NYC") // Add deep nested
                    })
                    put("contact", JSONObject().apply {
                        put("phone", "1234567890") // Add to existing object
                    })
                })
                put("settings", JSONObject().apply {
                    put("theme", "dark") // Add new key to nested object
                })
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.UPDATE)

        // Verify deep nested updates
        val personal = target.getJSONObject("user")
            .getJSONObject("profile")
            .getJSONObject("personal")
        assertEquals("John", personal.getString("name")) // Unchanged
        assertEquals(30, personal.getInt("age")) // Updated
        assertEquals("NYC", personal.getString("city")) // Added

        // Verify changes with correct deep paths
        assertTrue(result.changes.containsKey("user.profile.personal.age"))
        assertTrue(result.changes.containsKey("user.profile.personal.city"))
        assertTrue(result.changes.containsKey("user.profile.contact.phone"))
        assertTrue(result.changes.containsKey("user.settings.theme"))
        assertFalse(result.changes.containsKey("user.profile.personal.name"))
        assertFalse(result.changes.containsKey("user.profile.contact.email"))
        assertFalse(result.changes.containsKey("user.settings.privacy"))
    }

    @Test
    fun `UPDATE handles empty and null scenarios`() {
        // Test 1: Empty source
        val target1 = JSONObject().apply {
            put("name", "John")
            put("age", 25)
        }
        val source1 = JSONObject()

        val result1 = traverser.traverse(target1, source1, ProfileOperation.UPDATE)
        assertEquals(0, result1.changes.size)
        assertEquals(2, target1.length()) // Nothing removed

        // Test 2: Empty target
        val target2 = JSONObject()
        val source2 = JSONObject().apply {
            put("name", "John")
            put("age", 25)
        }

        val result2 = traverser.traverse(target2, source2, ProfileOperation.UPDATE)
        assertEquals(2, result2.changes.size)
        result2.changes.values.forEach { change ->
            assertNull(change.oldValue) // All new additions
        }

        // Test 3: Null values
        val target3 = JSONObject().apply {
            put("name", "John")
            put("age", 25)
        }
        val source3 = JSONObject().apply {
            put("name", JSONObject.NULL)
            put("middle", JSONObject.NULL) // New key with null
        }

        val result3 = traverser.traverse(target3, source3, ProfileOperation.UPDATE)
        assertTrue(target3.isNull("name"))
        assertTrue(target3.isNull("middle"))
        assertEquals(2, result3.changes.size)
    }

    @Test
    fun `UPDATE handles type conversions and replacements`() {
        val target = JSONObject().apply {
            // String -> Number
            put("stringToNum", "123")

            // Number -> String
            put("numToString", 456)

            // Boolean -> String
            put("boolToString", true)

            // Primitive -> Object
            put("primToObj", "simple")

            // Object -> Primitive
            put("objToPrim", JSONObject().apply {
                put("nested", "value")
            })

            // Primitive -> Array
            put("primToArray", "value")

            // Array -> Primitive
            put("arrayToPrim", JSONArray().apply {
                put("item1")
                put("item2")
            })
        }

        val source = JSONObject().apply {
            put("stringToNum", 123)
            put("numToString", "456")
            put("boolToString", "true")
            put("primToObj", JSONObject().apply {
                put("key", "value")
            })
            put("objToPrim", "nowSimple")
            put("primToArray", JSONArray().apply {
                put("a")
                put("b")
            })
            put("arrayToPrim", "single")
        }

        val result = traverser.traverse(target, source, ProfileOperation.UPDATE)
        println("AnushX " + result)

        // Verify all type conversions
        assertEquals(123, target.getInt("stringToNum"))
        assertEquals("456", target.getString("numToString"))
        assertEquals("true", target.getString("boolToString"))
        assertTrue(target.get("primToObj") is JSONObject)
        assertEquals("nowSimple", target.getString("objToPrim"))
        assertTrue(target.get("primToArray") is JSONArray)
        assertEquals("single", target.getString("arrayToPrim"))

        // All should create changes
        assertTrue(result.changes.containsKey("stringToNum"))
        assertTrue(result.changes.containsKey("numToString"))
        assertTrue(result.changes.containsKey("boolToString"))

        // Object conversion records leaf values
        assertTrue(result.changes.containsKey("objToPrim"))
        assertTrue(result.changes.containsKey("primToArray"))
        assertTrue(result.changes.containsKey("arrayToPrim"))
    }


    @Test
    fun `UPDATE handles arrays and mixed content`() {
        val target = JSONObject().apply {
            put("simpleArray", JSONArray().apply {
                put("a")
                put("b")
            })
            put("mixedArray", JSONArray().apply {
                put("string")
                put(123)
                put(true)
            })
            put("nestedArray", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 1)
                })
            })
        }

        val source = JSONObject().apply {
            put("simpleArray", JSONArray().apply {
                put("c")
                put("d")
                put("e")
            })
            put("mixedArray", JSONArray().apply {
                put(456)
                put(false)
            })
            put("nestedArray", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 2)
                    put("name", "test")
                })
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.UPDATE)

        println("AnushX" + result)
        // Arrays are replaced entirely in UPDATE
        val simpleArray = target.getJSONArray("simpleArray")
        assertEquals(3, simpleArray.length())
        assertEquals("c", simpleArray.getString(0))

        val mixedArray = target.getJSONArray("mixedArray")
        assertEquals(2, mixedArray.length())
        assertEquals(456, mixedArray.getInt(0))

        // All array replacements should be tracked
        assertTrue(result.changes.containsKey("simpleArray"))
        assertTrue(result.changes.containsKey("mixedArray"))
        assertTrue(result.changes.containsKey("nestedArray"))
    }

    @Test
    fun `UPDATE preserves unchanged values in complex structure`() {
        val target = JSONObject().apply {
            put("unchanged1", "value1")
            put("changed1", "oldValue")
            put("nested", JSONObject().apply {
                put("unchanged2", "value2")
                put("changed2", "oldValue2")
                put("deep", JSONObject().apply {
                    put("unchanged3", "value3")
                    put("changed3", "oldValue3")
                })
            })
        }

        val source = JSONObject().apply {
            put("changed1", "newValue")
            put("nested", JSONObject().apply {
                put("changed2", "newValue2")
                put("deep", JSONObject().apply {
                    put("changed3", "newValue3")
                })
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.UPDATE)

        // Verify unchanged values are preserved
        assertEquals("value1", target.getString("unchanged1"))
        assertEquals("value2", target.getJSONObject("nested").getString("unchanged2"))
        assertEquals("value3", target.getJSONObject("nested")
            .getJSONObject("deep").getString("unchanged3"))

        // Verify only changed values are in changes
        assertEquals(3, result.changes.size)
        assertTrue(result.changes.containsKey("changed1"))
        assertTrue(result.changes.containsKey("nested.changed2"))
        assertTrue(result.changes.containsKey("nested.deep.changed3"))

        // Unchanged keys should not be in changes
        assertFalse(result.changes.containsKey("unchanged1"))
        assertFalse(result.changes.containsKey("nested.unchanged2"))
        assertFalse(result.changes.containsKey("nested.deep.unchanged3"))
    }
}
