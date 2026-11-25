/*
 * Copyright 2014 Google, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.clevertap.android.sdk.gif

@Suppress("unused")
internal class GifHeader {

    var bgColor = 0

    // Background color index.
    var bgIndex = 0

    var currentFrame: GifFrame? = null

    var frameCount = 0

    var frames: MutableList<GifFrame> = ArrayList()

    var gct: IntArray? = null

    // 1 : global color table flag.
    var gctFlag = false

    // 2-4 : color resolution.
    // 5 : gct sort flag.
    // 6-8 : gct size.
    var gctSize = 0

    // Full image height.
    var height = 0

    var loopCount = 0

    // Pixel aspect ratio.
    var pixelAspect = 0

    /**
     * Global status code of GIF data parsing.
     */
    var status = GifDecoder.STATUS_OK

    // Logical screen size.
    // Full image width.
    var width = 0

    fun getNumFrames(): Int {
        return frameCount
    }
}
