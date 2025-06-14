package com.clevertap.android.sdk.inapp

import android.graphics.Bitmap
import android.os.Handler
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate
import com.clevertap.android.sdk.inapp.customtemplates.TemplateArgument
import com.clevertap.android.sdk.inapp.customtemplates.TemplateArgumentType
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoImpl
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.utils.handlerMock
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InAppNotificationInflaterTest {

    companion object {
        private const val FILE_URL_1 = "https://example.com/file1.jpeg"
        private const val FILE_URL_2 = "https://example.com/file2.jpeg"
    }

    private lateinit var mockStoreRegistry: StoreRegistry
    private lateinit var mockFileResourceProvider: FileResourceProvider
    private lateinit var mockHandler: Handler
    private lateinit var mockTemplateManager: TemplatesManager


    @Before
    fun setUp() {
        mockStoreRegistry = mockk<StoreRegistry>()
        every { mockStoreRegistry.filesStore } returns mockk()
        every { mockStoreRegistry.inAppAssetsStore } returns mockk()

        mockFileResourceProvider = mockk<FileResourceProvider>()
        mockTemplateManager = mockk<TemplatesManager>()

        mockHandler = handlerMock()

        mockkObject(FileResourcesRepoImpl)
        every { FileResourcesRepoImpl.saveUrlExpiryToStore(any(), any()) } just Runs
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `inflate should set error when invalid json is provided`() {
        val inflater = createInflater()
        val listener = object : InAppNotificationInflater.InAppNotificationReadyListener {
            override fun onNotificationReady(notification: CTInAppNotification) {
                assertNotNull(notification.error)
            }
        }
        val listenerSpy = spyk(listener)
        inflater.inflate(JSONObject(), "Test", listenerSpy)
        verify(exactly = 1) { listenerSpy.onNotificationReady(any()) }
    }

    @Test
    fun `inflate should initiate fetch file for all custom template file args`() {
        val templateJson = setupCustomTemplateWithFileArgs()
        val inflater = createInflater()
        inflater.inflate(templateJson, "Test") { inApp ->
            assertNull(inApp.error)
        }

        verify(exactly = 1) { mockFileResourceProvider.fetchFile(FILE_URL_1) }
        verify(exactly = 1) { mockFileResourceProvider.fetchFile(FILE_URL_2) }
    }

    @Test
    fun `inflate should not initiate fetch file for custom templates without file args`() {
        val mockTemplate = mockk<CustomTemplate>()
        every { mockTemplate.args } returns emptyList()
        every { mockTemplateManager.getTemplate(any()) } returns mockTemplate

        val templateJson = JSONObject(
            """
        {
            "type": "custom-code",
            "templateName": "test-template",
            "vars": {
                "stringArg": "string"
            }
        }
        """.trimIndent()
        )

        val inflater = createInflater()
        inflater.inflate(templateJson, "Test") { inApp ->
            assertNull(inApp.error)
        }

        verify(exactly = 0) { mockFileResourceProvider.fetchFile(any()) }
    }

    @Test
    fun `inflate should set error when custom template file is not downloaded`() {
        val templateJson = setupCustomTemplateWithFileArgs()
        val inflater = createInflater(shouldFetchFilesSuccessfully = false)
        inflater.inflate(templateJson, "Test") { inApp ->
            assertNotNull(inApp.error)
        }
    }

    @Test
    fun `inflate should initiate media fetch for in-apps`() {
        val inAppJson = JSONObject(
            """
            {
                "type": "interstitial",
                "media": {
                    "url": "$FILE_URL_1",
                    "content_type": "image/jpeg"
                },
                "mediaLandscape": {
                     "url": "$FILE_URL_2",
                    "content_type": "image/gif"
                }
            }
        """.trimIndent()
        )

        val inflater = createInflater()
        inflater.inflate(inAppJson, "Test") { inApp ->
            assertNull(inApp.error)
        }

        verify(exactly = 1) { mockFileResourceProvider.fetchInAppImageV1(FILE_URL_1) }
        verify(exactly = 1) { mockFileResourceProvider.fetchInAppGifV1(FILE_URL_2) }
    }

    @Test
    fun `inflate should set error when in-apps contain video media and video is not supported`() {
        val inAppJson = JSONObject(
            """
            {
                "type": "interstitial",
                "media": {
                    "url": "https://example.com/video.mp4",
                    "content_type": "video/mp4"
                }
            }
        """.trimIndent()
        )

        val inflater = createInflater(isVideoSupported = false)
        inflater.inflate(inAppJson, "Test") { inApp ->
            assertNotNull(inApp.error)
        }

        confirmVerified(mockFileResourceProvider)
    }

    @Test
    fun `inflate should set error when image fetch for in-apps fails`() {
        val inAppJson = JSONObject(
            """
            {
                "type": "interstitial",
                "media": {
                    "url": "$FILE_URL_1",
                    "content_type": "image/jpeg"
                }
            }
        """.trimIndent()
        )

        val inflater = createInflater(shouldFetchFilesSuccessfully = false)
        inflater.inflate(inAppJson, "Test") { inApp ->
            assertNotNull(inApp.error)
        }

        verify(exactly = 1) { mockFileResourceProvider.fetchInAppImageV1(FILE_URL_1) }
    }

    @Test
    fun `inflate should set error when gif fetch for in-apps fails`() {
        val inAppJson = JSONObject(
            """
            {
                "type": "interstitial",
                "media": {
                    "url": "$FILE_URL_1",
                    "content_type": "image/gif"
                }
            }
        """.trimIndent()
        )

        val inflater = createInflater(shouldFetchFilesSuccessfully = false)
        inflater.inflate(inAppJson, "Test") { inApp ->
            assertNotNull(inApp.error)
        }

        verify(exactly = 1) { mockFileResourceProvider.fetchInAppGifV1(FILE_URL_1) }
    }

    private fun setupCustomTemplateWithFileArgs(): JSONObject {
        val mockTemplate = mockk<CustomTemplate>()
        val fileArgs = listOf<TemplateArgument>(
            TemplateArgument("file1", TemplateArgumentType.FILE, null),
            TemplateArgument("file2", TemplateArgumentType.FILE, null)
        )
        every { mockTemplate.args } returns fileArgs
        every { mockTemplateManager.getTemplate(any()) } returns mockTemplate

        return JSONObject(
            """
        {
            "type": "custom-code",
            "templateName": "test-template",
            "vars": {
                "file1": "$FILE_URL_1",
                "file2": "$FILE_URL_2"
            }
        }
        """.trimIndent()
        )
    }

    private fun createInflater(
        shouldFetchFilesSuccessfully: Boolean = true,
        isVideoSupported: Boolean = true
    ): InAppNotificationInflater {
        val byteArray: ByteArray?
        val bitmap: Bitmap?
        if (shouldFetchFilesSuccessfully) {
            byteArray = byteArrayOf(1, 2, 3)
            bitmap = mockk()
        } else {
            byteArray = null
            bitmap = null
        }
        every { mockFileResourceProvider.fetchFile(any()) } returns byteArray
        every { mockFileResourceProvider.fetchInAppGifV1(any()) } returns byteArray
        every { mockFileResourceProvider.fetchInAppImageV1(any()) } returns bitmap

        return InAppNotificationInflater(
            mockStoreRegistry,
            mockTemplateManager,
            MockCTExecutors(),
            mockHandler,
            mockFileResourceProvider,
            isVideoSupported
        )
    }
}
