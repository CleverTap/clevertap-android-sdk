package com.clevertap.android.sdk.inapp.pipsdk.internal.engine

import android.graphics.PointF
import com.clevertap.android.sdk.inapp.pipsdk.PIPPosition

internal object PIPPositionResolver {

    /** Returns pixel (x, y) top-left coordinates for the PIP view at each position. */
    fun resolveAnchors(
        containerWidth: Int,
        containerHeight: Int,
        pipWidth: Int,
        pipHeight: Int,
        horizontalMarginPx: Int,
        verticalMarginPx: Int,
    ): Map<PIPPosition, PointF> = buildMap {
        val midX = (containerWidth - pipWidth) / 2f
        val midY = (containerHeight - pipHeight) / 2f
        val left = horizontalMarginPx.toFloat()
        val top = verticalMarginPx.toFloat()
        val right = (containerWidth - pipWidth - horizontalMarginPx).toFloat()
        val bottom = (containerHeight - pipHeight - verticalMarginPx).toFloat()

        put(PIPPosition.TOP_LEFT,      PointF(left,  top))
        put(PIPPosition.TOP_CENTER,    PointF(midX,  top))
        put(PIPPosition.TOP_RIGHT,     PointF(right, top))
        put(PIPPosition.LEFT_CENTER,   PointF(left,  midY))
        put(PIPPosition.CENTER,        PointF(midX,  midY))
        put(PIPPosition.RIGHT_CENTER,  PointF(right, midY))
        put(PIPPosition.BOTTOM_LEFT,   PointF(left,  bottom))
        put(PIPPosition.BOTTOM_CENTER, PointF(midX,  bottom))
        put(PIPPosition.BOTTOM_RIGHT,  PointF(right, bottom))
    }

    /** Finds the position whose anchor centre is closest to (pipCenterX, pipCenterY). */
    fun nearestPosition(
        pipCenterX: Float,
        pipCenterY: Float,
        anchors: Map<PIPPosition, PointF>,
        pipWidth: Int,
        pipHeight: Int,
    ): PIPPosition {
        return anchors.minByOrNull { (_, anchor) ->
            val dx = pipCenterX - (anchor.x + pipWidth / 2f)
            val dy = pipCenterY - (anchor.y + pipHeight / 2f)
            dx * dx + dy * dy   // squared distance — no sqrt needed
        }?.key ?: PIPPosition.BOTTOM_RIGHT
    }
}
