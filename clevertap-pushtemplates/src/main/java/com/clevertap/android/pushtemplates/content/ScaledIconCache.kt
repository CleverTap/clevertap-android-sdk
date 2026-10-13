package com.clevertap.android.pushtemplates.content

import android.graphics.Bitmap
import com.clevertap.android.pushtemplates.PTLog

/**
 * Display-sized notification icons, kept for as long as the process lives.
 *
 * [com.clevertap.android.pushtemplates.media.TemplateMediaManager] caches per instance and is
 * cleared at the end of every render, and a notification tap builds a fresh one — so without this
 * every tap would re-download every icon before the row could be redrawn. That costs a visible delay
 * between the tap and the highlight, and on a flaky network it is worse than slow: a download that
 * fails mid-interaction makes the row fall back to the built-in stars, so a row of hearts changes
 * shape while the user is tapping it.
 *
 * What is stored is the bitmap *after* it has been scaled to the size it is drawn at, which is what
 * makes keeping it affordable: an icon for a 48dp cell is a few tens of kilobytes, so the whole cache
 * is smaller than one unscaled campaign asset.
 *
 * Entries are owned by the cache. A caller must never recycle a bitmap it got from here — the next
 * render would find a recycled bitmap and have to download again, which is the problem this exists
 * to solve.
 */
internal object ScaledIconCache {

    /**
     * Bitmaps kept at once. Five positions have two states each, so ten covers a single campaign's
     * whole row with room for a second campaign alongside it.
     */
    private const val MAX_ENTRIES = 24

    /** Access-ordered, so the entries a live notification keeps asking for are the last to go. */
    private val entries = object : LinkedHashMap<String, Bitmap>(0, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean =
            size > MAX_ENTRIES
    }

    /** Renders run on a background executor and two notifications can be drawn at once. */
    private val lock = Any()

    private fun key(url: String, maxSidePx: Int) = "$url@$maxSidePx"

    /**
     * The cached bitmap for [url] at [maxSidePx], or null when it has not been fetched yet.
     *
     * A recycled entry is dropped rather than returned: something outside the cache recycled it, and
     * handing it back would draw nothing.
     */
    fun get(url: String, maxSidePx: Int): Bitmap? = synchronized(lock) {
        val cached = entries[key(url, maxSidePx)] ?: return null
        if (cached.isRecycled) {
            PTLog.debug("Cached icon for $url was recycled elsewhere, dropping it")
            entries.remove(key(url, maxSidePx))
            return null
        }
        return cached
    }

    fun put(url: String, maxSidePx: Int, bitmap: Bitmap) = synchronized(lock) {
        if (bitmap.isRecycled) return
        entries[key(url, maxSidePx)] = bitmap
    }

    /** Test seam. Nothing in the SDK clears this — the entries are small and the process is short. */
    internal fun clear() = synchronized(lock) { entries.clear() }

    internal fun size(): Int = synchronized(lock) { entries.size }
}
