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

import com.clevertap.android.sdk.Logger
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays

internal class GifHeaderParser {

    // Raw data read working array.
    private val block = ByteArray(MAX_BLOCK_SIZE)

    private var blockSize = 0

    private var header: GifHeader? = null

    private var rawData: ByteBuffer? = null

    fun clear() {
        rawData = null
        header = null
    }

    /**
     * Determines if the GIF is animated by trying to read in the first 2 frames
     * This method reparses the data even if the header has already been read.
     */
    @Suppress("unused")
    fun isAnimated(): Boolean {
        readHeader()
        if (!err()) {
            readContents(2 /* maxFrames */)
        }
        return header!!.frameCount > 1
    }

    fun setData(data: ByteBuffer): GifHeaderParser {
        reset()
        val rawData = data.asReadOnlyBuffer()
        this.rawData = rawData
        rawData.position(0)
        rawData.order(ByteOrder.LITTLE_ENDIAN)
        return this
    }

    fun setData(data: ByteArray?): GifHeaderParser {
        if (data != null) {
            setData(ByteBuffer.wrap(data))
        } else {
            rawData = null
            header!!.status = GifDecoder.STATUS_OPEN_ERROR
        }
        return this
    }

    fun parseHeader(): GifHeader? {
        if (rawData == null) {
            throw IllegalStateException("You must call setData() before parseHeader()")
        }
        if (err()) {
            return header
        }

        readHeader()
        if (!err()) {
            readContents()
            if (header!!.frameCount < 0) {
                header!!.status = GifDecoder.STATUS_FORMAT_ERROR
            }
        }

        return header
    }

    private fun err(): Boolean {
        return header!!.status != GifDecoder.STATUS_OK
    }

    /**
     * Reads a single byte from the input stream.
     */
    private fun read(): Int {
        var curByte = 0
        try {
            curByte = rawData!!.get().toInt() and 0xFF
        } catch (e: Exception) {
            header!!.status = GifDecoder.STATUS_FORMAT_ERROR
        }
        return curByte
    }

    /**
     * Reads next frame image.
     */
    private fun readBitmap() {
        val header = header!!
        // (sub)image position & size.
        header.currentFrame!!.ix = readShort()
        header.currentFrame!!.iy = readShort()
        header.currentFrame!!.iw = readShort()
        header.currentFrame!!.ih = readShort()

        val packed = read()
        // 1 - local color table flag interlace
        val lctFlag = (packed and 0x80) != 0
        val lctSize = Math.pow(2.0, ((packed and 0x07) + 1).toDouble()).toInt()
        // 3 - sort flag
        // 4-5 - reserved lctSize = 2 << (packed & 7); // 6-8 - local color
        // table size
        header.currentFrame!!.interlace = (packed and 0x40) != 0
        if (lctFlag) {
            // Read table.
            header.currentFrame!!.lct = readColorTable(lctSize)
        } else {
            // No local color table.
            header.currentFrame!!.lct = null
        }

        // Save this as the decoding position pointer.
        header.currentFrame!!.bufferFrameStart = rawData!!.position()

        // False decode pixel data to advance buffer.
        skipImageData()

        if (err()) {
            return
        }

        header.frameCount++
        // Add image to frame.
        header.frames.add(header.currentFrame!!)
    }

    /**
     * Reads next variable length block from input.
     *
     * @return number of bytes stored in "buffer"
     */
    @Suppress("UnusedReturnValue")
    private fun readBlock(): Int {
        blockSize = read()
        var n = 0
        if (blockSize > 0) {
            var count: Int
            try {
                while (n < blockSize) {
                    count = blockSize - n
                    rawData!!.get(block, n, count)

                    n += count
                }
            } catch (e: Exception) {
//                if (Log.isLoggable(TAG, Log.DEBUG)) {
//                    Log.d(TAG,
//                            "Error Reading Block n: " + n + " count: " + count + " blockSize: " + blockSize, e);
//                }
                header!!.status = GifDecoder.STATUS_FORMAT_ERROR
            }
        }
        return n
    }

    /**
     * Reads color table as 256 RGB integer values.
     *
     * @param ncolors int number of colors to read.
     * @return int array containing 256 colors (packed ARGB with full alpha).
     */
    private fun readColorTable(ncolors: Int): IntArray? {
        val nbytes = 3 * ncolors
        var tab: IntArray? = null
        val c = ByteArray(nbytes)

        try {
            rawData!!.get(c)

            // Max size to avoid bounds checks.
            tab = IntArray(MAX_BLOCK_SIZE)
            var i = 0
            var j = 0
            while (i < ncolors) {
                val r = c[j++].toInt() and 0xff
                val g = c[j++].toInt() and 0xff
                val b = c[j++].toInt() and 0xff
                tab[i++] = OPAQUE_ALPHA or (r shl 16) or (g shl 8) or b
            }
        } catch (e: BufferUnderflowException) {
            //if (Log.isLoggable(TAG, Log.DEBUG)) {
            Logger.d(TAG, "Format Error Reading Color Table", e)
            //}
            header!!.status = GifDecoder.STATUS_FORMAT_ERROR
        }

        return tab
    }

    /**
     * Main file parser. Reads GIF content blocks.
     */
    private fun readContents() {
        readContents(Int.MAX_VALUE /* maxFrames */)
    }

    /**
     * Main file parser. Reads GIF content blocks. Stops after reading maxFrames
     */
    private fun readContents(maxFrames: Int) {
        // Read GIF file content blocks.
        var done = false
        while (!(done || err() || header!!.frameCount > maxFrames)) {
            var code = read()
            when (code) {
                // Image separator.
                0x2C -> {
                    // The graphics control extension is optional, but will always come first if it exists.
                    // If one did
                    // exist, there will be a non-null current frame which we should use. However if one
                    // did not exist,
                    // the current frame will be null and we must create it here. See issue #134.
                    if (header!!.currentFrame == null) {
                        header!!.currentFrame = GifFrame()
                    }
                    readBitmap()
                }
                // Extension.
                0x21 -> {
                    code = read()
                    when (code) {
                        // Graphics control extension.
                        0xf9 -> {
                            // Start a new frame.
                            header!!.currentFrame = GifFrame()
                            readGraphicControlExt()
                        }
                        // Application extension.
                        0xff -> {
                            readBlock()
                            var app = ""
                            for (i in 0 until 11) {
                                app += block[i].toInt().toChar()
                            }
                            if (app == "NETSCAPE2.0") {
                                readNetscapeExt()
                            } else {
                                // Don't care.
                                skip()
                            }
                        }
                        // Comment extension.
                        0xfe -> skip()
                        // Plain text extension.
                        0x01 -> skip()
                        // Uninteresting extension.
                        else -> skip()
                    }
                }
                // Terminator.
                0x3b -> done = true
                // Bad byte, but keep going and see what happens break;
                0x00 -> header!!.status = GifDecoder.STATUS_FORMAT_ERROR
                else -> header!!.status = GifDecoder.STATUS_FORMAT_ERROR
            }
        }
    }

    /**
     * Reads Graphics Control Extension values.
     */
    private fun readGraphicControlExt() {
        val currentFrame = header!!.currentFrame!!
        // Block size.
        read()
        // Packed fields.
        val packed = read()
        // Disposal method.
        currentFrame.dispose = (packed and 0x1c) shr 2
        if (currentFrame.dispose == 0) {
            // Elect to keep old image if discretionary.
            currentFrame.dispose = 1
        }
        currentFrame.transparency = (packed and 1) != 0
        // Delay in milliseconds.
        var delayInHundredthsOfASecond = readShort()

        if (delayInHundredthsOfASecond < MIN_FRAME_DELAY) {
            delayInHundredthsOfASecond = DEFAULT_FRAME_DELAY
        }
        currentFrame.delay = delayInHundredthsOfASecond * 10
        // Transparent color index
        currentFrame.transIndex = read()
        // Block terminator
        read()
    }

    /**
     * Reads GIF file header information.
     */
    private fun readHeader() {
        var id = ""
        for (i in 0 until 6) {
            id += read().toChar()
        }
        if (!id.startsWith("GIF")) {
            header!!.status = GifDecoder.STATUS_FORMAT_ERROR
            return
        }
        readLSD()
        if (header!!.gctFlag && !err()) {
            header!!.gct = readColorTable(header!!.gctSize)
            header!!.bgColor = header!!.gct!![header!!.bgIndex]
        }
    }

    /**
     * Reads Logical Screen Descriptor.
     */
    private fun readLSD() {
        val header = header!!
        // Logical screen size.
        header.width = readShort()
        header.height = readShort()
        // Packed fields
        val packed = read()
        // 1 : global color table flag.
        header.gctFlag = (packed and 0x80) != 0
        // 2-4 : color resolution.
        // 5 : gct sort flag.
        // 6-8 : gct size.
        header.gctSize = 2 shl (packed and 7)
        // Background color index.
        header.bgIndex = read()
        // Pixel aspect ratio
        header.pixelAspect = read()
    }

    /**
     * Reads Netscape extension to obtain iteration count.
     */
    private fun readNetscapeExt() {
        do {
            readBlock()
            if (block[0].toInt() == 1) {
                // Loop count sub-block.
                val b1 = block[1].toInt() and 0xff
                val b2 = block[2].toInt() and 0xff
                header!!.loopCount = (b2 shl 8) or b1
                if (header!!.loopCount == 0) {
                    header!!.loopCount = GifDecoder.LOOP_FOREVER
                }
            }
        } while ((blockSize > 0) && !err())
    }

    /**
     * Reads next 16-bit value, LSB first.
     */
    private fun readShort(): Int {
        // Read 16-bit value.
        return rawData!!.getShort().toInt()
    }

    private fun reset() {
        rawData = null
        Arrays.fill(block, 0.toByte())
        header = GifHeader()
        blockSize = 0
    }

    /**
     * Skips variable length blocks up to and including next zero length block.
     */
    private fun skip() {
        try {
            var blockSize: Int
            do {
                blockSize = read()
                rawData!!.position(rawData!!.position() + blockSize)
            } while (blockSize > 0)
        } catch (ex: IllegalArgumentException) {
            // no-op
        }
    }

    /**
     * Skips LZW image data for a single frame to advance buffer.
     */
    private fun skipImageData() {
        // lzwMinCodeSize
        read()
        // data sub-blocks
        skip()
    }

    private companion object {

        private const val TAG = "GifHeaderParser"

        // The minimum frame delay in hundredths of a second.
        private const val MIN_FRAME_DELAY = 2

        // The default frame delay in hundredths of a second for GIFs with frame delays less than the
        // minimum.
        private const val DEFAULT_FRAME_DELAY = 10

        private const val MAX_BLOCK_SIZE = 256

        // 0xff000000 as a (negative) Int, as in the Java original.
        private const val OPAQUE_ALPHA = 0xff000000.toInt()
    }
}
