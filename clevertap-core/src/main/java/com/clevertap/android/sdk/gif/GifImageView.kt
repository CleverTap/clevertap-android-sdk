package com.clevertap.android.sdk.gif

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

@Suppress("unused")
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

    private var animationStartCallback: OnAnimationStart? = null

    private var animationStopCallback: OnAnimationStop? = null

    private var animationThread: Thread? = null

    private var frameCallback: OnFrameAvailable? = null

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
        if (tmpBitmap != null && !tmpBitmap!!.isRecycled) {
            setImageBitmap(tmpBitmap)
        }
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context)

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
        get() = gifDecoder!!.getFrameCount()

    open val gifHeight: Int
        get() = gifDecoder!!.getHeight()

    open val gifWidth: Int
        get() = gifDecoder!!.getWidth()

    open var onAnimationStop: OnAnimationStop?
        get() = animationStopCallback
        set(animationStop) {
            this.animationStopCallback = animationStop
        }

    open var onFrameAvailable: OnFrameAvailable?
        get() = frameCallback
        set(frameProcessor) {
            this.frameCallback = frameProcessor
        }

    open fun gotoFrame(frame: Int) {
        if (gifDecoder!!.getCurrentFrameIndex() == frame) {
            return
        }
        if (gifDecoder!!.setFrameIndex(frame - 1) && !animating) {
            renderFrame = true
            startAnimationThread()
        }
    }

    open val isAnimating: Boolean
        get() = animating

    open fun resetAnimation() {
        gifDecoder!!.resetLoopIndex()
        gotoFrame(0)
    }

    override fun run() {
        animationStartCallback?.onAnimationStart()

        do {
            if (!animating && !renderFrame) {
                break
            }
            val advance = gifDecoder!!.advance()

            //milliseconds spent on frame decode
            var frameDecodeTime: Long = 0
            try {
                val before = System.nanoTime()
                tmpBitmap = gifDecoder!!.getNextFrame()
                if (frameCallback != null) {
                    tmpBitmap = frameCallback!!.onFrameAvailable(tmpBitmap)
                }
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
                var delay = gifDecoder!!.getNextDelay()
                // Sleep for frame duration minus time already spent on frame decode
                // Actually we need next frame decode duration here,
                // but I use previous frame time to make code more readable
                delay = (delay - frameDecodeTime).toInt()
                if (delay > 0) {
                    Thread.sleep(if (framesDisplayDuration > 0) framesDisplayDuration else delay.toLong())
                }
            } catch (e: InterruptedException) {
                // suppress exception
            }
        } while (animating)

        if (shouldClear) {
            handler.post(cleanupRunnable)
        }
        animationThread = null

        animationStopCallback?.onAnimationStop()
    }

    open fun setBytes(bytes: ByteArray?) {
        gifDecoder = GifDecoder()
        try {
            gifDecoder!!.read(bytes)
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

    open fun setOnAnimationStart(animationStart: OnAnimationStart?) {
        this.animationStartCallback = animationStart
    }

    open fun startAnimation() {
        animating = true
        startAnimationThread()
    }

    open fun stopAnimation() {
        animating = false

        if (animationThread != null) {
            animationThread!!.interrupt()
            animationThread = null
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clear()
    }

    private fun canStart(): Boolean {
        return (animating || renderFrame) && gifDecoder != null && animationThread == null
    }

    private fun startAnimationThread() {
        if (canStart()) {
            animationThread = Thread(this)
            animationThread!!.start()
        }
    }

    private companion object {
        private const val TAG = "GifDecoderView"
    }
}
