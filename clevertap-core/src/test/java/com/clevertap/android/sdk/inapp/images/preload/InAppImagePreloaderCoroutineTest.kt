package com.clevertap.android.sdk.inapp.images.preload

import TestDispatchers
import android.graphics.Bitmap
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.inapp.images.InAppResourceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.Mockito

class InAppImagePreloaderCoroutineTest {

    //@get:Rule
    //val mainDispatcherRule = MainDispatcherRule()

    private val mockBitmap = Mockito.mock(Bitmap::class.java)
    private val inAppResourceProvider = Mockito.mock(InAppResourceProvider::class.java)
    private val logger = TestLogger()

    private val testScheduler = TestCoroutineScheduler()
    private val dispatchers = TestDispatchers(testScheduler)

    private val inAppImagePreloaderCoroutine = InAppImagePreloaderCoroutine(
        inAppImageProvider = inAppResourceProvider,
        logger = logger,
        dispatchers = dispatchers
    )

    @Test
    fun `preload image fetches images from all urls`() = testScheduler.run {

        val urls = mutableListOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k")

        for (url in urls) {
            Mockito.`when`(inAppResourceProvider.fetchInAppImage(url)).thenReturn(mockBitmap)
        }

        inAppImagePreloaderCoroutine.preloadImages(urls)
        advanceUntilIdle()

        for (url in urls) {
            Mockito.verify(inAppResourceProvider).fetchInAppImage(url)
        }
    }
}

class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
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