package com.clevertap.android.sdk.gif

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.annotation.RestrictTo

@Suppress("unused")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class GifImageView : AppCompatImageView, Runnable {

    fun interface OnFrameAvailable {

        fun onFrameAvailable(bitmap: Bitmap?): Bitmap?
    }

    fun interface OnAnimationStop {

        fun onAnimationStop()
    }

    fun interface OnAnimationStart {

        fun onAnimationStart()
    }

    private var animating = false

    private var animationThread: Thread? = null

    /**
     * Custom display duration in milliseconds for the all frames. Should be set before [startAnimation].
     * Default value = -1, this property will be ignored and default delay from gif file will be used.
     */
    open var framesDisplayDuration = -1L

    private var gifDecoder: GifDecoder? = null

    private val handler = Handler(Looper.getMainLooper())

    private var renderFrame = false

    private var shouldClear = false

    private var tmpBitmap: Bitmap? = null

    private val cleanupRunnable = Runnable {
        tmpBitmap = null
        gifDecoder = null
        animationThread = null
        shouldClear = false
    }

    private val updateResults = Runnable {
        val bitmap = tmpBitmap
        if (bitmap != null && !bitmap.isRecycled) {
            setImageBitmap(bitmap)
        }
    }

    // Inflated from XML layouts: keep the framework constructors.
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context)

    /** The decoder of the current GIF; using the view before [setBytes] succeeded fails here, as before. */
    private val decoder: GifDecoder
        get() = checkNotNull(gifDecoder) { "No GIF decoded" }

    open fun clear() {
        animating = false
        renderFrame = false
        shouldClear = true
        stopAnimation()
        handler.post(cleanupRunnable)
    }

    /**
     * Gets the number of frames read from file.
     *
     * @return frame count.
     */
    open val frameCount: Int
        get() = decoder.getFrameCount()

    open val gifHeight: Int
        get() = decoder.getHeight()

    open val gifWidth: Int
        get() = decoder.getWidth()

    open var onAnimationStart: OnAnimationStart? = null

    open var onAnimationStop: OnAnimationStop? = null

    open var onFrameAvailable: OnFrameAvailable? = null

    open fun gotoFrame(frame: Int) {
        val decoder = decoder
        if (decoder.getCurrentFrameIndex() == frame) {
            return
        }
        if (decoder.setFrameIndex(frame - 1) && !animating) {
            renderFrame = true
            startAnimationThread()
        }
    }

    open val isAnimating: Boolean
        get() = animating

    open fun resetAnimation() {
        decoder.resetLoopIndex()
        gotoFrame(0)
    }

    override fun run() {
        onAnimationStart?.onAnimationStart()

        do {
            if (!animating && !renderFrame) {
                break
            }
            val advance = decoder.advance()

            //milliseconds spent on frame decode
            var frameDecodeTime: Long = 0
            try {
                val before = System.nanoTime()
                tmpBitmap = decoder.getNextFrame()
                onFrameAvailable?.let { tmpBitmap = it.onFrameAvailable(tmpBitmap) }
                frameDecodeTime = (System.nanoTime() - before) / 1000000
                handler.post(updateResults)
            } catch (e: ArrayIndexOutOfBoundsException) {
                //Log.w(TAG, e);
            } catch (e: IllegalArgumentException) {
                //Log.w(TAG, e);
            }

            renderFrame = false
            if (!animating || !advance) {
                animating = false
                break
            }
            try {
                // Sleep for frame duration minus time already spent on frame decode
                // Actually we need next frame decode duration here,
                // but I use previous frame time to make code more readable
                val delay = (decoder.getNextDelay() - frameDecodeTime).toInt()
                if (delay > 0) {
                    // Paces GIF frames on the view's own animation thread, never the SDK or main thread.
                    @Suppress("ForbiddenMethodCall")
                    Thread.sleep(if (framesDisplayDuration > 0) { framesDisplayDuration } else { delay.toLong() })
                }
            } catch (e: InterruptedException) {
                // suppress exception
            }
        } while (animating)

        if (shouldClear) {
            handler.post(cleanupRunnable)
        }
        animationThread = null

        onAnimationStop?.onAnimationStop()
    }

    open fun setBytes(bytes: ByteArray?) {
        val decoder = GifDecoder()
        gifDecoder = decoder
        try {
            decoder.read(bytes)
        } catch (e: Exception) {
            gifDecoder = null
            //Log.e(TAG, e.getMessage(), e);
            return
        }

        if (animating) {
            startAnimationThread()
        } else {
            gotoFrame(0)
        }
    }

    open fun startAnimation() {
        animating = true
        startAnimationThread()
    }

    open fun stopAnimation() {
        animating = false
        animationThread?.interrupt()
        animationThread = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clear()
    }

    private fun canStart(): Boolean = (animating || renderFrame) && gifDecoder != null && animationThread == null

    private fun startAnimationThread() {
        if (canStart()) {
            val thread = Thread(this)
            animationThread = thread
            thread.start()
        }
    }

    private companion object {
        private const val TAG = "GifDecoderView"
    }
}
