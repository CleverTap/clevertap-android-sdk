package com.clevertap.android.sdk.inapp.images.preload

import TestDispatchers
import android.graphics.Bitmap
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.test.assertEquals

class FilePreloaderCoroutineTest {

    //@get:Rule
    //val mainDispatcherRule = MainDispatcherRule()

    private val mockBitmap = mockk<Bitmap>()
    private val byteArray = ByteArray(10) { pos ->
        pos.toByte()
    }
    private val mFileResourceProvider = mockk<FileResourceProvider>()
    private val logger = TestLogger()

    private val testScheduler = TestCoroutineScheduler()
    private val dispatchers = TestDispatchers(testScheduler)

    private val filePreloaderCoroutine = FilePreloaderCoroutine(
        fileResourceProvider = mFileResourceProvider,
        logger = logger,
        dispatchers = dispatchers
    )

    @Test
    fun `preload image fetches images from all urls`() = testScheduler.run {

        val urls = mutableListOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k").map { Pair(it,
            CtCacheType.IMAGE) }
        val successUrls = mutableListOf<String>()

        urls.forEach{
            every {
                mFileResourceProvider.fetchInAppImageV1(it.first)
            } returns mockBitmap
        }

        val func = fun (url: Pair<String, CtCacheType>) {
            // dummy func
            successUrls.add(url.first)
        }

        filePreloaderCoroutine.preloadFilesAndCache(urls, func,{},{},{})
        advanceUntilIdle()

        urls.forEach{
            verify {
                mFileResourceProvider.fetchInAppImageV1(it.first)
            }
        }
        assertEquals(urls.size, successUrls.size)
    }

    @Test
    fun `preload gifs fetches gif from all urls`() = testScheduler.run {

        val urls = mutableListOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k")
            .map { Pair(it, CtCacheType.GIF) }
        val successUrls = mutableListOf<String>()

        urls.forEach{
            every {
                mFileResourceProvider.fetchInAppGifV1(it.first)
            } returns byteArray
        }

        val func = fun (url: Pair<String, CtCacheType>) {
            // dummy func
            successUrls.add(url.first)
        }

        filePreloaderCoroutine.preloadFilesAndCache(urls, func,{},{},{})
        advanceUntilIdle()

        urls.forEach{
            verify {
                mFileResourceProvider.fetchInAppGifV1(it.first)
            }
        }
        assertEquals(urls.size, successUrls.size)
    }

    @Test
    fun `preload files fetches files from all urls`() = testScheduler.run {
        val urls = mutableListOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k")
            .map { Pair(it, CtCacheType.FILES) }
        val successUrls = mutableListOf<String>()

        // replace with forEach
        urls.forEach{
            every {
                mFileResourceProvider.fetchFile(it.first)
            } returns byteArray
        }

        filePreloaderCoroutine.preloadFilesAndCache(urls, { url ->
            successUrls.add(url.first)
        },{},{},{})
        advanceUntilIdle()

        urls.forEach {
            verify {
                mFileResourceProvider.fetchFile(it.first)
            }
        }
        assertEquals(urls.size, successUrls.size)

    }

    @Test
    fun `preloadFilesAndCache invokes all callbacks for images`() = testScheduler.run {
        val urls = listOf("a", "b", "c").map { Pair(it, CtCacheType.IMAGE) }
        val successUrls = mutableListOf<String>()
        val failureUrls = mutableListOf<String>()
        val startedUrls = mutableListOf<String>()
        val finishedStatus = mutableMapOf<String, Boolean>()

        urls.forEach {
            every { mFileResourceProvider.fetchInAppImageV1(it.first) } returns
                    if (it.first == "b") null else mockBitmap // Simulate failure for "b"
        }

        filePreloaderCoroutine.preloadFilesAndCache(
            urls,
            successBlock = { url -> successUrls.add(url.first) },
            failureBlock = { url -> failureUrls.add(url.first) },
            startedBlock = { url -> startedUrls.add(url.first) },
            preloadFinished = { status -> finishedStatus.putAll(status) }
        )
        advanceUntilIdle()

        assertEquals(listOf("a", "c"), successUrls)
        assertEquals(listOf("b"), failureUrls)
        assertEquals(listOf("a", "b", "c"), startedUrls)
        assertEquals(mapOf("a" to true, "b" to false, "c" to true), finishedStatus)
    }

    @Test
    fun `preloadFilesAndCache invokes all callbacks for GIFs`() = testScheduler.run {
        val urls = listOf("x", "y", "z").map { Pair(it, CtCacheType.GIF) }
        val successUrls = mutableListOf<String>()
        val failureUrls = mutableListOf<String>()
        val startedUrls = mutableListOf<String>()
        val finishedStatus = mutableMapOf<String, Boolean>()

        urls.forEach {
            every { mFileResourceProvider.fetchInAppGifV1(it.first) } returns
                    if (it.first == "y") null else byteArray // Simulate failure for "y"
        }

        filePreloaderCoroutine.preloadFilesAndCache(
            urls,
            successBlock = { url -> successUrls.add(url.first) },
            failureBlock = { url -> failureUrls.add(url.first) },
            startedBlock = { url -> startedUrls.add(url.first) },
            preloadFinished = { status -> finishedStatus.putAll(status) }
        )
        advanceUntilIdle()

        assertEquals(listOf("x", "z"), successUrls)
        assertEquals(listOf("y"), failureUrls)
        assertEquals(listOf("x", "y", "z"), startedUrls)
        assertEquals(mapOf("x" to true, "y" to false, "z" to true), finishedStatus)

    }
    @Test
    fun `preloadFilesAndCache invokes all callbacks for files`() = testScheduler.run {
        val urls = listOf("p", "q", "r").map { Pair(it, CtCacheType.FILES) }
        val successUrls = mutableListOf<String>()
        val failureUrls = mutableListOf<String>()
        val startedUrls = mutableListOf<String>()
        val finishedStatus = mutableMapOf<String, Boolean>()
        urls.forEach {
            every { mFileResourceProvider.fetchFile(it.first) } returns
                    if (it.first == "q") null else byteArray //Simulate failure for "q"
        }
        filePreloaderCoroutine.preloadFilesAndCache(
            urls,
            successBlock = { url -> successUrls.add(url.first) },
            failureBlock = { url -> failureUrls.add(url.first) },
            startedBlock = { url -> startedUrls.add(url.first) },
            preloadFinished = { status -> finishedStatus.putAll(status) }
        )
        advanceUntilIdle()

        assertEquals(listOf("p", "r"), successUrls)
        assertEquals(listOf("q"), failureUrls)
        assertEquals(listOf("p", "q", "r"), startedUrls)
        assertEquals(mapOf("p" to true, "q" to false, "r" to true), finishedStatus)
    }

    @Test
    fun `check results in case of timeout along with failures`() = testScheduler.run {

        // Prepare data - setup, Given :
        val filePreloaderCoroutineWithTimeout = FilePreloaderCoroutine(
            fileResourceProvider = mFileResourceProvider,
            logger = logger,
            dispatchers = dispatchers,
            timeoutForPreload = 100 // 100ms
        )

        val filesList = buildList {
            add("p" to CtCacheType.FILES)
            add("q" to CtCacheType.FILES)
            add("r" to CtCacheType.FILES)
        }

        val imagesList = buildList {
            add("a" to CtCacheType.IMAGE)
            add("b" to CtCacheType.IMAGE)
            add("c" to CtCacheType.IMAGE)
        }

        val gifsList = buildList {
            add("m" to CtCacheType.GIF)
            add("n" to CtCacheType.GIF)
            add("o" to CtCacheType.GIF)
        }

        val urls = filesList + imagesList + gifsList

        val finishedStatus = mutableMapOf<String, Boolean>()

        every { mFileResourceProvider.fetchFile("p") } returns byteArray
        every { mFileResourceProvider.fetchFile("q") } returns byteArray
        every { mFileResourceProvider.fetchFile("r") } returns null // Simulate failure for "r"
        every { mFileResourceProvider.fetchInAppImageV1("a") } returns mockBitmap
        every { mFileResourceProvider.fetchInAppImageV1("b") } returns mockBitmap
        every { mFileResourceProvider.fetchInAppImageV1("c") } returns null
        every { mFileResourceProvider.fetchInAppGifV1("m") } returns byteArray
        every { mFileResourceProvider.fetchInAppGifV1("n") } returns byteArray
        every { mFileResourceProvider.fetchInAppGifV1("o") } returns null

        val successfulExpected = mutableSetOf("p", "q", "a", "b", "m", "n") // expected success
        val failedExpected = mutableSetOf("c", "r", "o") // expected failures
        val startedExpected = mutableSetOf("p", "q", "r", "a", "b", "c", "m", "n", "o")
        val finishedExpected = mapOf(
            "p" to true,
            "q" to true,
            "r" to false,
            "a" to true,
            "b" to true,
            "c" to false,
            "m" to true,
            "n" to true,
            "o" to false,
        ) // expected final map of statuses

        // Function to test :
        filePreloaderCoroutineWithTimeout.preloadFilesAndCache(
            urlMetas = urls,
            successBlock = { url -> successfulExpected.remove(url.first) },
            failureBlock = { url -> failedExpected.remove(url.first) },
            startedBlock = { url -> startedExpected.remove(url.first) },
            preloadFinished = { status -> finishedStatus.putAll(status) }
        )
        advanceUntilIdle()

        // Assertions :
        assertEquals(expected = 0, actual = successfulExpected.size)
        assertEquals(expected = 0, actual = failedExpected.size)
        assertEquals(expected = 0, actual = startedExpected.size)
        assertEquals(
            expected = finishedExpected,
            actual = finishedStatus
        )
    }
}

class MainDispatcherRule @OptIn(ExperimentalCoroutinesApi::class) constructor(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}