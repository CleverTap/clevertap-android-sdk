package com.clevertap.android.sdk.network

import android.content.Context
import android.content.SharedPreferences
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.Logger
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ArpRepoTest : BaseTestCase() {

    private val accountId = "arp-test-account"
    private val deviceId = "arp-test-device"
    private val newKey = "ARP:$accountId:$deviceId"
    private val oldKey = "ARP:$accountId"

    private lateinit var arpRepo: ArpRepo

    @Before
    override fun setUp() {
        super.setUp()
        val deviceInfo = mockk<DeviceInfo>(relaxed = true)
        every { deviceInfo.deviceID } returns deviceId
        arpRepo = ArpRepo(accountId = accountId, logger = mockk(relaxed = true), deviceInfo = deviceInfo)
        prefs(newKey).edit().clear().commit()
        prefs(oldKey).edit().clear().commit()
    }

    private fun prefs(key: String): SharedPreferences =
        appCtx.getSharedPreferences("WizRocket_$key", Context.MODE_PRIVATE)

    @Test
    fun `handleARPUpdate stores supported values and rejects the rest`() {
        val arp = JSONObject()
            .put("int", 5)
            .put("double", 7.9)
            .put("string", "value")
            .put("longString", "x".repeat(100))
            .put("bool", true)
            .put("array", JSONArray().put(1))

        arpRepo.handleARPUpdate(appCtx, arp)

        val stored = prefs(newKey).all
        assertEquals(5, stored["int"])
        assertEquals(7, stored["double"])
        assertEquals("value", stored["string"])
        assertEquals(true, stored["bool"])
        assertFalse(stored.containsKey("longString"))
        assertFalse(stored.containsKey("array"))
        assertEquals(4, stored.size)
    }

    @Test
    fun `handleARPUpdate with empty arp writes nothing`() {
        arpRepo.handleARPUpdate(appCtx, JSONObject())

        assertTrue(prefs(newKey).all.isEmpty())
    }

    @Test
    fun `getARP returns new namespace values without entries set to -1`() {
        prefs(newKey).edit()
            .putInt("keep", 3)
            .putInt("drop", -1)
            .putString("name", "n")
            .commit()

        val arp = arpRepo.getARP(appCtx)

        assertNotNull(arp)
        assertEquals(3, arp.getInt("keep"))
        assertEquals("n", arp.getString("name"))
        assertFalse(arp.has("drop"))
    }

    @Test
    fun `getARP migrates old namespace when new namespace is empty`() {
        prefs(oldKey).edit()
            .putInt("int", 2)
            .putLong("long", 9L)
            .putString("string", "s")
            .putString("longString", "y".repeat(150))
            .putBoolean("bool", false)
            .putStringSet("set", setOf("a"))
            .commit()

        val arp = arpRepo.getARP(appCtx)

        assertNotNull(arp)
        assertEquals(2, arp.getInt("int"))
        assertEquals(9, arp.getInt("long"))
        assertEquals("s", arp.getString("string"))
        assertEquals(false, arp.getBoolean("bool"))
        assertFalse(arp.has("longString"))
        assertFalse(arp.has("set"))

        val migrated = prefs(newKey).all
        assertEquals(setOf("int", "long", "string", "bool"), migrated.keys)
        assertTrue(prefs(oldKey).all.isEmpty())
    }
}
