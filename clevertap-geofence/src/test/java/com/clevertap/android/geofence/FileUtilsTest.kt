package com.clevertap.android.geofence

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileUtilsTest : BaseTestCase() {

    private lateinit var logger: Logger

    override fun setUp() {
        super.setUp()
        logger = mockk(relaxed = true)
        mockkObject(CTGeofenceAPI.Companion)
        every { CTGeofenceAPI.getLogger() } returns logger
    }

    @After
    override fun cleanUp() {
        File(application.filesDir, DIR_NAME).deleteRecursively()
        super.cleanUp()
        unmockkObject(CTGeofenceAPI.Companion)
    }

    @Test
    fun deleteDirectory_deletesEveryChildAndKeepsTheDirectory() {
        val dir = File(application.filesDir, DIR_NAME).apply { mkdirs() }
        File(dir, "a.json").writeText("{}")
        File(dir, "b.json").writeText("{}")

        FileUtils.deleteDirectory(context = application, dirName = DIR_NAME)

        assertTrue(dir.exists())
        assertEquals(0, dir.list()!!.size)
        verify { logger.verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG, "a.json :successfully deleted") }
        verify { logger.verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG, "b.json :successfully deleted") }
    }

    @Test
    fun deleteDirectory_doesNothingWhenDirectoryIsMissing() {
        FileUtils.deleteDirectory(context = application, dirName = DIR_NAME)

        assertFalse(File(application.filesDir, DIR_NAME).exists())
        verify(exactly = 0) { logger.verbose(any(), any<String>()) }
    }

    @Test
    fun writeJsonToFile_createsMissingDirectoryAndWritesJson() {
        val json = JSONObject().put("id", 1)

        val result = FileUtils.writeJsonToFile(
            context = application,
            dirName = DIR_NAME,
            fileName = FILE_NAME,
            jsonObject = json
        )

        assertTrue(result)
        assertEquals(json.toString(), File(File(application.filesDir, DIR_NAME), FILE_NAME).readText())
    }

    @Test
    fun writeJsonToFile_overwritesFileInExistingDirectory() {
        val dir = File(application.filesDir, DIR_NAME).apply { mkdirs() }
        File(dir, FILE_NAME).writeText("old")
        val json = JSONObject().put("id", 2)

        val result = FileUtils.writeJsonToFile(
            context = application,
            dirName = DIR_NAME,
            fileName = FILE_NAME,
            jsonObject = json
        )

        assertTrue(result)
        assertEquals(json.toString(), File(dir, FILE_NAME).readText())
    }

    @Test
    fun writeJsonToFile_returnsFalseWhenJsonIsNull() {
        val result = FileUtils.writeJsonToFile(
            context = application,
            dirName = DIR_NAME,
            fileName = FILE_NAME,
            jsonObject = null
        )

        assertFalse(result)
        assertFalse(File(application.filesDir, DIR_NAME).exists())
    }

    private companion object {
        const val DIR_NAME = "file_utils_test_dir"
        const val FILE_NAME = "test.json"
    }
}
