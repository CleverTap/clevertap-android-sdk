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


    @Test
    fun `INCREMENT adds to existing numeric values`() {
        val target = JSONObject().apply {
            put("score", 100)
            put("points", 50.5)
            put("level", 5)
            put("bonus", 0)
        }

        val source = JSONObject().apply {
            put("score", 25)
            put("points", 10.5)
            put("level", 2)
            put("bonus", 100)
        }

        val result = traverser.traverse(target, source, ProfileOperation.INCREMENT)

        // Verify incremented values
        assertEquals(125, target.getInt("score"))
        assertEquals(61.0, target.getDouble("points"), 0.001)
        assertEquals(7, target.getInt("level"))
        assertEquals(100, target.getInt("bonus"))

        // Verify changes tracked
        assertEquals(4, result.changes.size)
        assertEquals(100, result.changes["score"]!!.oldValue)
        assertEquals(125, result.changes["score"]!!.newValue)
        assertEquals(50.5, result.changes["points"]!!.oldValue)
        assertEquals(61.0, result.changes["points"]!!.newValue)
        assertEquals(5, result.changes["level"]!!.oldValue)
        assertEquals(7, result.changes["level"]!!.newValue)
        assertEquals(0, result.changes["bonus"]!!.oldValue)
        assertEquals(100, result.changes["bonus"]!!.newValue)
    }

    @Test
    fun `INCREMENT adds new keys that don't exist in target`() {
        val target = JSONObject().apply {
            put("existingScore", 100)
        }

        val source = JSONObject().apply {
            put("newScore", 50)
            put("newPoints", 25.5)
            put("anotherValue", 10)
        }

        val result = traverser.traverse(target, source, ProfileOperation.INCREMENT)

        // Verify new keys are added with source values
        assertEquals(50, target.getInt("newScore"))
        assertEquals(25.5, target.getDouble("newPoints"), 0.001)
        assertEquals(10, target.getInt("anotherValue"))

        // Verify changes tracked with null oldValue
        assertTrue(result.changes.containsKey("newScore"))
        assertNull(result.changes["newScore"]!!.oldValue)
        assertEquals(50, result.changes["newScore"]!!.newValue)

        assertTrue(result.changes.containsKey("newPoints"))
        assertNull(result.changes["newPoints"]!!.oldValue)
        assertEquals(25.5, result.changes["newPoints"]!!.newValue)
    }

    @Test
    fun `INCREMENT skips non-numeric values in target`() {
        val target = JSONObject().apply {
            put("score", 100)
            put("name", "John") // String
            put("active", true) // Boolean
            put("tags", JSONArray().apply { put("tag1") }) // Array
            put("metadata", JSONObject().apply { put("key", "value") }) // Object
        }

        val source = JSONObject().apply {
            put("score", 50)
            put("name", 10) // Try to increment string
            put("active", 5) // Try to increment boolean
            put("tags", 20) // Try to increment array
            put("metadata", 15) // Try to increment object
        }

        val result = traverser.traverse(target, source, ProfileOperation.INCREMENT)

        // Only numeric target value should be incremented
        assertEquals(150, target.getInt("score"))

        // Non-numeric values should remain unchanged
        assertEquals("John", target.getString("name"))
        assertEquals(true, target.getBoolean("active"))
        assertTrue(target.get("tags") is JSONArray)
        assertTrue(target.get("metadata") is JSONObject)

        // Only score should be in changes
        assertEquals(1, result.changes.size)
        assertTrue(result.changes.containsKey("score"))
    }

    @Test
    fun `INCREMENT skips non-numeric values in source`() {
        val target = JSONObject().apply {
            put("score", 100)
            put("points", 50)
        }

        val source = JSONObject().apply {
            put("score", 25) // Valid
            put("points", "invalid") // Invalid - string
            put("newValue1", true) // Invalid - boolean
            put("newValue2", JSONArray()) // Invalid - array
            put("newValue3", JSONObject()) // Invalid - object
        }

        val result = traverser.traverse(target, source, ProfileOperation.INCREMENT)

        // Only valid numeric increment should work
        assertEquals(125, target.getInt("score"))
        assertEquals(50, target.getInt("points")) // Unchanged

        // Invalid source values should not be added
        assertFalse(target.has("newValue1"))
        assertFalse(target.has("newValue2"))
        assertFalse(target.has("newValue3"))

        // Only score should be in changes
        assertEquals(1, result.changes.size)
        assertTrue(result.changes.containsKey("score"))
    }

    @Test
    fun `INCREMENT handles nested objects`() {
        val target = JSONObject().apply {
            put("stats", JSONObject().apply {
                put("wins", 10)
                put("losses", 5)
                put("score", 100.5)
            })
            put("progress", JSONObject().apply {
                put("level", 3)
                put("xp", 500)
            })
        }

        val source = JSONObject().apply {
            put("stats", JSONObject().apply {
                put("wins", 3)
                put("losses", 1)
            })
            put("progress", JSONObject().apply {
                put("xp", 150)
                put("bonus", 50) // New key
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.INCREMENT)

        // Verify incremented nested values
        val stats = target.getJSONObject("stats")
        assertEquals(13, stats.getInt("wins"))
        assertEquals(6, stats.getInt("losses"))
        assertEquals(100.5, stats.getDouble("score"), 0.001) // Unchanged

        val progress = target.getJSONObject("progress")
        assertEquals(3, progress.getInt("level")) // Unchanged
        assertEquals(650, progress.getInt("xp"))
        assertEquals(50, progress.getInt("bonus")) // New key added

        // Verify changes with dot notation
        assertTrue(result.changes.containsKey("stats.wins"))
        assertEquals(10, result.changes["stats.wins"]!!.oldValue)
        assertEquals(13, result.changes["stats.wins"]!!.newValue)

        assertTrue(result.changes.containsKey("stats.losses"))
        assertTrue(result.changes.containsKey("progress.xp"))
        assertTrue(result.changes.containsKey("progress.bonus"))
        assertNull(result.changes["progress.bonus"]!!.oldValue)

        // Unchanged values should not be in changes
        assertFalse(result.changes.containsKey("stats.score"))
        assertFalse(result.changes.containsKey("progress.level"))
    }

    @Test
    fun `INCREMENT handles deeply nested numeric values`() {
        val target = JSONObject().apply {
            put("game", JSONObject().apply {
                put("player", JSONObject().apply {
                    put("stats", JSONObject().apply {
                        put("health", 100)
                        put("mana", 50)
                    })
                })
            })
        }

        val source = JSONObject().apply {
            put("game", JSONObject().apply {
                put("player", JSONObject().apply {
                    put("stats", JSONObject().apply {
                        put("health", 25)
                        put("mana", 10)
                        put("stamina", 30) // New key
                    })
                })
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.INCREMENT)

        // Verify deep nested increments
        val stats = target.getJSONObject("game")
            .getJSONObject("player")
            .getJSONObject("stats")
        assertEquals(125, stats.getInt("health"))
        assertEquals(60, stats.getInt("mana"))
        assertEquals(30, stats.getInt("stamina"))

        // Verify changes with deep paths
        assertTrue(result.changes.containsKey("game.player.stats.health"))
        assertTrue(result.changes.containsKey("game.player.stats.mana"))
        assertTrue(result.changes.containsKey("game.player.stats.stamina"))
    }

    @Test
    fun `INCREMENT handles negative numbers`() {
        val target = JSONObject().apply {
            put("balance", -50)
            put("debt", -100)
            put("score", 25)
        }

        val source = JSONObject().apply {
            put("balance", 100) // Add positive to negative
            put("debt", -25) // Add negative to negative
            put("score", -10) // Add negative to positive
        }

        val result = traverser.traverse(target, source, ProfileOperation.INCREMENT)

        assertEquals(50, target.getInt("balance"))
        assertEquals(-125, target.getInt("debt"))
        assertEquals(15, target.getInt("score"))

        assertEquals(3, result.changes.size)
    }

    @Test
    fun `INCREMENT handles mixed integer and double values`() {
        val target = JSONObject().apply {
            put("intValue", 10)
            put("doubleValue", 5.5)
            put("mixedValue", 100)
        }

        val source = JSONObject().apply {
            put("intValue", 5.5) // Add double to int
            put("doubleValue", 10) // Add int to double
            put("mixedValue", 2.3) // Add double to int
        }

        val result = traverser.traverse(target, source, ProfileOperation.INCREMENT)

        // Values should be added correctly
        assertEquals(15.5, target.getDouble("intValue"), 0.001)
        assertEquals(15.5, target.getDouble("doubleValue"), 0.001)
        assertEquals(102.3, target.getDouble("mixedValue"), 0.001)
    }

    @Test
    fun `INCREMENT handles zero values`() {
        val target = JSONObject().apply {
            put("value1", 0)
            put("value2", 100)
        }

        val source = JSONObject().apply {
            put("value1", 50) // Add to zero
            put("value2", 0) // Add zero
            put("value3", 0) // Add zero to missing key
        }

        val result = traverser.traverse(target, source, ProfileOperation.INCREMENT)

        assertEquals(50, target.getInt("value1"))
        assertEquals(100, target.getInt("value2")) // No change
        assertEquals(0, target.getInt("value3"))

        // value2 should not be in changes (no actual change)
        assertTrue(result.changes.containsKey("value1"))
        assertFalse(result.changes.containsKey("value2"))
        assertTrue(result.changes.containsKey("value3"))
    }

    @Test
    fun `INCREMENT handles comprehensive scenario`() {
        val target = JSONObject().apply {
            put("score", 100)
            put("name", "John") // Non-numeric
            put("stats", JSONObject().apply {
                put("wins", 10)
                put("title", "Champion") // Non-numeric nested
                put("points", 50.5)
            })
        }

        val source = JSONObject().apply {
            put("score", 25)
            put("name", 5) // Try to increment string
            put("level", 3) // New key
            put("stats", JSONObject().apply {
                put("wins", 5)
                put("losses", 2) // New nested key
                put("points", 10.5)
                put("title", 1) // Try to increment nested string
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.INCREMENT)

        // Top-level increments
        assertEquals(125, target.getInt("score"))
        assertEquals("John", target.getString("name")) // Unchanged
        assertEquals(3, target.getInt("level")) // New key added

        // Nested increments
        val stats = target.getJSONObject("stats")
        assertEquals(15, stats.getInt("wins"))
        assertEquals(2, stats.getInt("losses")) // New key added
        assertEquals(61.0, stats.getDouble("points"), 0.001)
        assertEquals("Champion", stats.getString("title")) // Unchanged

        // Verify changes
        assertTrue(result.changes.containsKey("score"))
        assertTrue(result.changes.containsKey("level"))
        assertTrue(result.changes.containsKey("stats.wins"))
        assertTrue(result.changes.containsKey("stats.losses"))
        assertTrue(result.changes.containsKey("stats.points"))
        assertFalse(result.changes.containsKey("name"))
        assertFalse(result.changes.containsKey("stats.title"))
    }

    // ==================== DECREMENT Operation Tests ====================

    @Test
    fun `DECREMENT subtracts from existing numeric values`() {
        val target = JSONObject().apply {
            put("score", 100)
            put("points", 50.5)
            put("level", 5)
            put("bonus", 200)
        }

        val source = JSONObject().apply {
            put("score", 25)
            put("points", 10.5)
            put("level", 2)
            put("bonus", 100)
        }

        val result = traverser.traverse(target, source, ProfileOperation.DECREMENT)

        // Verify decremented values
        assertEquals(75, target.getInt("score"))
        assertEquals(40.0, target.getDouble("points"), 0.001)
        assertEquals(3, target.getInt("level"))
        assertEquals(100, target.getInt("bonus"))

        // Verify changes tracked
        assertEquals(4, result.changes.size)
        assertEquals(100, result.changes["score"]!!.oldValue)
        assertEquals(75, result.changes["score"]!!.newValue)
        assertEquals(50.5, result.changes["points"]!!.oldValue)
        assertEquals(40.0, result.changes["points"]!!.newValue)
    }

    @Test
    fun `DECREMENT adds negated value for missing keys`() {
        val target = JSONObject().apply {
            put("existingScore", 100)
        }

        val source = JSONObject().apply {
            put("newScore", 50)
            put("newPoints", 25.5)
            put("anotherValue", 10)
        }

        val result = traverser.traverse(target, source, ProfileOperation.DECREMENT)

        // Verify new keys are added with negated source values
        assertEquals(-50, target.getInt("newScore"))
        assertEquals(-25.5, target.getDouble("newPoints"), 0.001)
        assertEquals(-10, target.getInt("anotherValue"))

        // Verify changes tracked with null oldValue
        assertTrue(result.changes.containsKey("newScore"))
        assertNull(result.changes["newScore"]!!.oldValue)
        assertEquals(-50, result.changes["newScore"]!!.newValue)

        assertTrue(result.changes.containsKey("newPoints"))
        assertNull(result.changes["newPoints"]!!.oldValue)
        assertEquals(-25.5, result.changes["newPoints"]!!.newValue)
    }

    @Test
    fun `DECREMENT skips non-numeric values in target`() {
        val target = JSONObject().apply {
            put("score", 100)
            put("name", "John") // String
            put("active", true) // Boolean
            put("tags", JSONArray().apply { put("tag1") }) // Array
            put("metadata", JSONObject().apply { put("key", "value") }) // Object
        }

        val source = JSONObject().apply {
            put("score", 50)
            put("name", 10) // Try to decrement string
            put("active", 5) // Try to decrement boolean
            put("tags", 20) // Try to decrement array
            put("metadata", 15) // Try to decrement object
        }

        val result = traverser.traverse(target, source, ProfileOperation.DECREMENT)

        // Only numeric target value should be decremented
        assertEquals(50, target.getInt("score"))

        // Non-numeric values should remain unchanged
        assertEquals("John", target.getString("name"))
        assertEquals(true, target.getBoolean("active"))
        assertTrue(target.get("tags") is JSONArray)
        assertTrue(target.get("metadata") is JSONObject)

        // Only score should be in changes
        assertEquals(1, result.changes.size)
        assertTrue(result.changes.containsKey("score"))
    }

    @Test
    fun `DECREMENT skips non-numeric values in source`() {
        val target = JSONObject().apply {
            put("score", 100)
            put("points", 50)
        }

        val source = JSONObject().apply {
            put("score", 25) // Valid
            put("points", "invalid") // Invalid - string
            put("newValue1", true) // Invalid - boolean
            put("newValue2", JSONArray()) // Invalid - array
            put("newValue3", JSONObject()) // Invalid - object
        }

        val result = traverser.traverse(target, source, ProfileOperation.DECREMENT)

        // Only valid numeric decrement should work
        assertEquals(75, target.getInt("score"))
        assertEquals(50, target.getInt("points")) // Unchanged

        // Invalid source values should not be added
        assertFalse(target.has("newValue1"))
        assertFalse(target.has("newValue2"))
        assertFalse(target.has("newValue3"))

        // Only score should be in changes
        assertEquals(1, result.changes.size)
        assertTrue(result.changes.containsKey("score"))
    }

    @Test
    fun `DECREMENT handles nested objects`() {
        val target = JSONObject().apply {
            put("stats", JSONObject().apply {
                put("health", 100)
                put("damage", 50)
                put("defense", 75.5)
            })
            put("resources", JSONObject().apply {
                put("gold", 500)
                put("gems", 100)
            })
        }

        val source = JSONObject().apply {
            put("stats", JSONObject().apply {
                put("health", 25)
                put("damage", 10)
            })
            put("resources", JSONObject().apply {
                put("gold", 150)
                put("silver", 50) // New key - should add -50
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.DECREMENT)

        // Verify decremented nested values
        val stats = target.getJSONObject("stats")
        assertEquals(75, stats.getInt("health"))
        assertEquals(40, stats.getInt("damage"))
        assertEquals(75.5, stats.getDouble("defense"), 0.001) // Unchanged

        val resources = target.getJSONObject("resources")
        assertEquals(350, resources.getInt("gold"))
        assertEquals(100, resources.getInt("gems")) // Unchanged
        assertEquals(-50, resources.getInt("silver")) // New key with negated value

        // Verify changes with dot notation
        assertTrue(result.changes.containsKey("stats.health"))
        assertTrue(result.changes.containsKey("stats.damage"))
        assertTrue(result.changes.containsKey("resources.gold"))
        assertTrue(result.changes.containsKey("resources.silver"))
        assertNull(result.changes["resources.silver"]!!.oldValue)

        // Unchanged values should not be in changes
        assertFalse(result.changes.containsKey("stats.defense"))
        assertFalse(result.changes.containsKey("resources.gems"))
    }

    @Test
    fun `DECREMENT handles deeply nested numeric values`() {
        val target = JSONObject().apply {
            put("game", JSONObject().apply {
                put("player", JSONObject().apply {
                    put("inventory", JSONObject().apply {
                        put("potions", 10)
                        put("scrolls", 5)
                    })
                })
            })
        }

        val source = JSONObject().apply {
            put("game", JSONObject().apply {
                put("player", JSONObject().apply {
                    put("inventory", JSONObject().apply {
                        put("potions", 3)
                        put("scrolls", 2)
                        put("arrows", 20) // New key
                    })
                })
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.DECREMENT)

        // Verify deep nested decrements
        val inventory = target.getJSONObject("game")
            .getJSONObject("player")
            .getJSONObject("inventory")
        assertEquals(7, inventory.getInt("potions"))
        assertEquals(3, inventory.getInt("scrolls"))
        assertEquals(-20, inventory.getInt("arrows")) // New key with negated value

        // Verify changes with deep paths
        assertTrue(result.changes.containsKey("game.player.inventory.potions"))
        assertTrue(result.changes.containsKey("game.player.inventory.scrolls"))
        assertTrue(result.changes.containsKey("game.player.inventory.arrows"))
    }

    @Test
    fun `DECREMENT handles negative numbers`() {
        val target = JSONObject().apply {
            put("balance", -50)
            put("debt", -100)
            put("score", 25)
        }

        val source = JSONObject().apply {
            put("balance", 100) // Subtract positive from negative
            put("debt", -25) // Subtract negative from negative
            put("score", -10) // Subtract negative from positive (adds)
        }

        val result = traverser.traverse(target, source, ProfileOperation.DECREMENT)

        assertEquals(-150, target.getInt("balance"))
        assertEquals(-75, target.getInt("debt"))
        assertEquals(35, target.getInt("score"))

        assertEquals(3, result.changes.size)
    }

    @Test
    fun `DECREMENT handles mixed integer and double values`() {
        val target = JSONObject().apply {
            put("intValue", 10)
            put("doubleValue", 15.5)
            put("mixedValue", 100)
        }

        val source = JSONObject().apply {
            put("intValue", 5.5) // Subtract double from int
            put("doubleValue", 10) // Subtract int from double
            put("mixedValue", 2.3) // Subtract double from int
        }

        val result = traverser.traverse(target, source, ProfileOperation.DECREMENT)

        // Values should be subtracted correctly
        assertEquals(4.5, target.getDouble("intValue"), 0.001)
        assertEquals(5.5, target.getDouble("doubleValue"), 0.001)
        assertEquals(97.7, target.getDouble("mixedValue"), 0.001)
    }

    @Test
    fun `DECREMENT handles comprehensive scenario`() {
        val target = JSONObject().apply {
            put("score", 100)
            put("name", "John") // Non-numeric
            put("inventory", JSONObject().apply {
                put("gold", 500)
                put("location", "Castle") // Non-numeric nested
                put("gems", 25.5)
            })
        }

        val source = JSONObject().apply {
            put("score", 25)
            put("name", 5) // Try to decrement string
            put("penalty", 10) // New key - should be -10
            put("inventory", JSONObject().apply {
                put("gold", 100)
                put("silver", 50) // New nested key - should be -50
                put("gems", 5.5)
                put("location", 1) // Try to decrement nested string
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.DECREMENT)

        // Top-level decrements
        assertEquals(75, target.getInt("score"))
        assertEquals("John", target.getString("name")) // Unchanged
        assertEquals(-10, target.getInt("penalty")) // New key with negated value

        // Nested decrements
        val inventory = target.getJSONObject("inventory")
        assertEquals(400, inventory.getInt("gold"))
        assertEquals(-50, inventory.getInt("silver")) // New key with negated value
        assertEquals(20.0, inventory.getDouble("gems"), 0.001)
        assertEquals("Castle", inventory.getString("location")) // Unchanged

        // Verify changes
        assertTrue(result.changes.containsKey("score"))
        assertTrue(result.changes.containsKey("penalty"))
        assertTrue(result.changes.containsKey("inventory.gold"))
        assertTrue(result.changes.containsKey("inventory.silver"))
        assertTrue(result.changes.containsKey("inventory.gems"))
        assertFalse(result.changes.containsKey("name"))
        assertFalse(result.changes.containsKey("inventory.location"))
    }

    // ==================== INCREMENT/DECREMENT Array Element Tests ====================

    @Test
    fun `INCREMENT handles array elements by position`() {
        val target = JSONObject().apply {
            put("scores", JSONArray().apply {
                put(30)
                put(20)
                put(50)
            })
        }

        val source = JSONObject().apply {
            put("scores", JSONArray().apply {
                put(null)
                put(10)
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.INCREMENT)

        // Verify incremented array elements
        val scores = target.getJSONArray("scores")
        assertEquals(3, scores.length())
        assertEquals(30, scores.getInt(0)) // 30 + null (null ignored)
        assertEquals(30, scores.getInt(1)) // 20 + 10
        assertEquals(50, scores.getInt(2)) // Unchanged (no corresponding source element)

        // Verify changes tracked
        assertTrue(result.changes.containsKey("scores"))

    }

    @Test
    fun `DECREMENT handles array elements by position`() {
        val target = JSONObject().apply {
            put("scores", JSONArray().apply {
                put(30)
                put(20)
                put(50)
            })
        }

        val source = JSONObject().apply {
            put("scores", JSONArray().apply {
                put(10)
                put(10)
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.DECREMENT)

        // Verify decremented array elements
        val scores = target.getJSONArray("scores")
        assertEquals(3, scores.length())
        assertEquals(20, scores.getInt(0)) // 30 - 10
        assertEquals(10, scores.getInt(1)) // 20 - 10
        assertEquals(50, scores.getInt(2)) // Unchanged

        // Verify changes tracked
        assertTrue(result.changes.containsKey("scores"))
    }

    @Test
    fun `INCREMENT skips non-numeric array elements in target`() {
        val target = JSONObject().apply {
            put("mixed", JSONArray().apply {
                put(30) // Numeric
                put("text") // String
                put(true) // Boolean
                put(JSONObject().apply { put("key", "value") }) // Object
                put(50) // Numeric
            })
        }

        val source = JSONObject().apply {
            put("mixed", JSONArray().apply {
                put(10)
                put(5)
                put(3)
                put(2)
                put(20)
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.INCREMENT)
        println("AnushX" + result)

        val mixed = target.getJSONArray("mixed")
        assertEquals(40, mixed.getInt(0)) // 30 + 10
        assertEquals("text", mixed.getString(1)) // Unchanged
        assertEquals(true, mixed.getBoolean(2)) // Unchanged
        assertTrue(mixed.get(3) is JSONObject) // Unchanged
        assertEquals(70, mixed.getInt(4)) // 50 + 20
    }

    @Test
    fun `DECREMENT skips non-numeric array elements in target`() {
        val target = JSONObject().apply {
            put("mixed", JSONArray().apply {
                put(100) // Numeric
                put("string") // String
                put(50) // Numeric
            })
        }

        val source = JSONObject().apply {
            put("mixed", JSONArray().apply {
                put(25)
                put(10)
                put(15)
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.DECREMENT)

        val mixed = target.getJSONArray("mixed")
        assertEquals(75, mixed.getInt(0)) // 100 - 25
        assertEquals("string", mixed.getString(1)) // Unchanged
        assertEquals(35, mixed.getInt(2)) // 50 - 15
    }

    @Test
    fun `INCREMENT skips non-numeric array elements in source`() {
        val target = JSONObject().apply {
            put("values", JSONArray().apply {
                put(10)
                put(20)
                put(30)
            })
        }

        val source = JSONObject().apply {
            put("values", JSONArray().apply {
                put(5) // Valid
                put("invalid") // Invalid - string
                put(true) // Invalid - boolean
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.INCREMENT)

        val values = target.getJSONArray("values")
        assertEquals(15, values.getInt(0)) // 10 + 5
        assertEquals(20, values.getInt(1)) // Unchanged (invalid source)
        assertEquals(30, values.getInt(2)) // Unchanged (invalid source)
    }

    @Test
    fun `DECREMENT skips non-numeric array elements in source`() {
        val target = JSONObject().apply {
            put("data", JSONArray().apply {
                put(100)
                put(50)
                put(75)
            })
        }

        val source = JSONObject().apply {
            put("data", JSONArray().apply {
                put(10) // Valid
                put(JSONObject()) // Invalid - object
                put(JSONArray()) // Invalid - array
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.DECREMENT)

        val data = target.getJSONArray("data")
        assertEquals(90, data.getInt(0)) // 100 - 10
        assertEquals(50, data.getInt(1)) // Unchanged
        assertEquals(75, data.getInt(2)) // Unchanged
    }

    @Test
    fun `INCREMENT handles nested arrays in objects`() {
        val target = JSONObject().apply {
            put("stats", JSONObject().apply {
                put("scores", JSONArray().apply {
                    put(10)
                    put(20)
                    put(30)
                })
                put("levels", JSONArray().apply {
                    put(1)
                    put(2)
                })
            })
        }

        val source = JSONObject().apply {
            put("stats", JSONObject().apply {
                put("scores", JSONArray().apply {
                    put(5)
                    put(10)
                })
                put("levels", JSONArray().apply {
                    put(1)
                })
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.INCREMENT)

        val scores = target.getJSONObject("stats").getJSONArray("scores")
        assertEquals(15, scores.getInt(0))
        assertEquals(30, scores.getInt(1))
        assertEquals(30, scores.getInt(2)) // Unchanged

        val levels = target.getJSONObject("stats").getJSONArray("levels")
        assertEquals(2, levels.getInt(0))
        assertEquals(2, levels.getInt(1)) // Unchanged

        // Verify changes with nested paths
        assertTrue(result.changes.containsKey("stats.scores"))
        assertTrue(result.changes.containsKey("stats.levels"))
    }

    @Test
    fun `DECREMENT handles nested arrays in objects`() {
        val target = JSONObject().apply {
            put("inventory", JSONObject().apply {
                put("items", JSONArray().apply {
                    put(100)
                    put(50)
                    put(75)
                })
            })
        }

        val source = JSONObject().apply {
            put("inventory", JSONObject().apply {
                put("items", JSONArray().apply {
                    put(25)
                    put(10)
                })
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.DECREMENT)

        val items = target.getJSONObject("inventory").getJSONArray("items")
        assertEquals(75, items.getInt(0))
        assertEquals(40, items.getInt(1))
        assertEquals(75, items.getInt(2)) // Unchanged
    }


    @Test
    fun `INCREMENT handles comprehensive array scenario`() {
        val target = JSONObject().apply {
            put("data", JSONObject().apply {
                put("metrics", JSONArray().apply {
                    put(100) // Numeric
                    put("text") // String (non-numeric)
                    put(50.5) // Numeric double
                    put(true) // Boolean (non-numeric)
                    put(25) // Numeric
                })
                put("simple", 200) // Simple numeric value
            })
        }

        val source = JSONObject().apply {
            put("data", JSONObject().apply {
                put("metrics", JSONArray().apply {
                    put(25) // Valid increment for index 0
                    put(5) // Try to increment string at index 1
                    put(10.5) // Valid increment for index 2
                    put("invalid") // Invalid source value at index 3
                    put(15) // Valid increment for index 4
                })
                put("simple", 50)
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.INCREMENT)

        // Verify array elements
        val metrics = target.getJSONObject("data").getJSONArray("metrics")
        assertEquals(125, metrics.getInt(0)) // 100 + 25
        assertEquals("text", metrics.getString(1)) // Unchanged (non-numeric target)
        assertEquals(61.0, metrics.getDouble(2), 0.001) // 50.5 + 10.5
        assertEquals(true, metrics.getBoolean(3)) // Unchanged (non-numeric target)
        assertEquals(40, metrics.getInt(4)) // 25 + 15

        // Verify simple value
        assertEquals(250, target.getJSONObject("data").getInt("simple"))

    }

    @Test
    fun `DECREMENT handles comprehensive array scenario`() {
        val target = JSONObject().apply {
            put("inventory", JSONObject().apply {
                put("quantities", JSONArray().apply {
                    put(100) // Numeric
                    put(JSONObject().apply { put("nested", "value") }) // Object (non-numeric)
                    put(75.5) // Numeric double
                    put(50) // Numeric
                })
                put("total", 500)
            })
        }

        val source = JSONObject().apply {
            put("inventory", JSONObject().apply {
                put("quantities", JSONArray().apply {
                    put(25) // Valid decrement
                    put(10) // Try to decrement object
                    put(JSONArray()) // Invalid source value
                    put(15) // Valid decrement
                })
                put("total", 100)
            })
        }

        val result = traverser.traverse(target, source, ProfileOperation.DECREMENT)

        val quantities = target.getJSONObject("inventory").getJSONArray("quantities")
        assertEquals(75, quantities.getInt(0)) // 100 - 25
        assertTrue(quantities.get(1) is JSONObject) // Unchanged (non-numeric target)
        assertEquals(75.5, quantities.getDouble(2), 0.001) // Unchanged (invalid source)
        assertEquals(35, quantities.getInt(3)) // 50 - 15

        assertEquals(400, target.getJSONObject("inventory").getInt("total"))
    }
}
