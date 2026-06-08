package com.seanproctor.signaturepad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.min

/**
 * Controls what happens to an in-progress signature when the [SignaturePad] is resized.
 *
 * Every behavior other than [Clear] is expressed as a mapping of each captured point from the old
 * coordinate space to the new one, so curves are remapped without losing their shape.
 */
public sealed interface ResizeBehavior {
    /** Discards the signature whenever the pad's size changes. This is the default. */
    public data object Clear : ResizeBehavior

    /** Keeps the signature at its original size and repositions it to stay centered. */
    public data object Center : ResizeBehavior

    /** Scales the signature uniformly (preserving aspect ratio) to fit the new size, then centers it. */
    public data object Fit : ResizeBehavior

    /** Scales the signature independently on each axis to fill the new size. May distort the drawing. */
    public data object Stretch : ResizeBehavior

    /**
     * Maps each point of the signature from the old space to the new one with a caller-supplied
     * function. The function receives a [point] in the old coordinate space along with the [oldSize]
     * and [newSize] of the pad, and returns the point's position in the new space.
     */
    public fun interface Custom : ResizeBehavior {
        public fun transformPoint(point: Offset, oldSize: Size, newSize: Size): Offset
    }
}

/** Maps a single point from [oldSize] space to [newSize] space according to this behavior. */
internal fun ResizeBehavior.mapPoint(point: Offset, oldSize: Size, newSize: Size): Offset =
    when (this) {
        // Clear never remaps points; callers handle it separately.
        ResizeBehavior.Clear -> point

        ResizeBehavior.Center -> Offset(
            point.x + (newSize.width - oldSize.width) / 2f,
            point.y + (newSize.height - oldSize.height) / 2f,
        )

        ResizeBehavior.Fit -> {
            val scale = min(newSize.width / oldSize.width, newSize.height / oldSize.height)
            Offset(
                point.x * scale + (newSize.width - oldSize.width * scale) / 2f,
                point.y * scale + (newSize.height - oldSize.height * scale) / 2f,
            )
        }

        ResizeBehavior.Stretch -> Offset(
            point.x * newSize.width / oldSize.width,
            point.y * newSize.height / oldSize.height,
        )

        is ResizeBehavior.Custom -> transformPoint(point, oldSize, newSize)
    }
