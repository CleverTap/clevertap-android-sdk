package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Shared `/a1` header vote-list logic used by both the in-app and ND evaluators (SDK-6055).
 */
class HeaderVoteListsTest {

    private val evalKey = Constants.INAPP_SS_EVAL_META
    private val suppressedKey = Constants.INAPP_SUPPRESSED_META

    @Test
    fun `readEvalIds restores int-range ids as Long (F3 regression)`() {
        // org.json parses int-range numbers as Integer; the parse must not drop them.
        val stored = JSONArray("[70001,70002]")
        assertEquals(mutableListOf(70001L, 70002L), HeaderVoteLists.readEvalIds(stored))
    }

    @Test
    fun `readEvalIds drops zero-valued entries`() {
        assertEquals(mutableListOf(70001L), HeaderVoteLists.readEvalIds(JSONArray("[70001,0]")))
    }

    @Test
    fun `attach returns null when both lists are empty`() {
        assertNull(HeaderVoteLists.attach(evalKey, suppressedKey, emptyList(), emptyList()))
    }

    @Test
    fun `attach includes only the non-empty lists`() {
        val header = HeaderVoteLists.attach(evalKey, suppressedKey, listOf(70001L), emptyList())
        assertEquals("[70001]", header!!.optJSONArray(evalKey).toString())
        assertTrue(!header.has(suppressedKey))
    }

    @Test
    fun `removeSentEvalIds removes exactly the sent ids and reports update`() {
        val list = mutableListOf(70001L, 70002L)
        val sent = JSONObject().put(evalKey, JSONArray().put(70001L))

        val updated = HeaderVoteLists.removeSentEvalIds(sent, evalKey, list)

        assertTrue(updated)
        assertEquals(mutableListOf(70002L), list)
    }

    @Test
    fun `removeSentEvalIds no-op when key absent`() {
        val list = mutableListOf(70001L)
        assertTrue(!HeaderVoteLists.removeSentEvalIds(JSONObject(), evalKey, list))
        assertEquals(mutableListOf(70001L), list)
    }

    @Test
    fun `removeSentSuppressed removes entries whose id appears in the sent header`() {
        val list = mutableListOf<Map<String, Any?>>(
            mapOf(Constants.NOTIFICATION_ID_TAG to "70003_20260810"),
            mapOf(Constants.NOTIFICATION_ID_TAG to "70004_20260810"),
        )
        val sent = JSONObject().put(
            suppressedKey,
            JSONArray().put(JSONObject().put(Constants.NOTIFICATION_ID_TAG, "70003_20260810")),
        )

        val updated = HeaderVoteLists.removeSentSuppressed(sent, suppressedKey, list)

        assertTrue(updated)
        assertEquals(1, list.size)
        assertEquals("70004_20260810", list[0][Constants.NOTIFICATION_ID_TAG])
    }
}
