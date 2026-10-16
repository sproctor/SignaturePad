package com.seanproctor.signaturepad

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.min

/**
 * The finished strokes of a signature, at the size of the pad they were drawn on. A [Signature] never
 * changes: [SignaturePadState.signature] holds a new one each time the finished strokes do. So it can
 * be read in composition, watched with `snapshotFlow`, and exported on any thread.
 *
 * @property width the width of the pad the signature was drawn on, in pixels, or 0 when it's empty.
 * @property height the height of the pad the signature was drawn on, in pixels, or 0 when it's empty.
 */
public class Signature internal constructor(
    internal val curves: PersistentList<Bezier>,
    public val width: Int,
    public val height: Int,
) {
    /** `true` when the signature has no strokes. */
    public val isEmpty: Boolean get() = curves.isEmpty()

    /**
     * Draws the signature onto [bitmap], scaled to fit it while preserving the aspect ratio. [penColor]
     * and [penWidth] are applied at the bitmap's resolution, not scaled with it.
     */
    public fun drawOnBitmap(bitmap: ImageBitmap, penColor: Color, penWidth: Float) {
        val scaling = min(bitmap.width / width.toFloat(), bitmap.height / height.toFloat())
        drawCurves(Canvas(bitmap), penPaint(penColor, penWidth), curves.map { it.scale(scaling) })
    }

    /**
     * Draws the signature onto a new bitmap the size of the pad, or returns `null` when it's empty. An
     * empty signature has no size, since the pad can be laid out or resized without publishing one.
     */
    public fun toImageBitmap(penColor: Color, penWidth: Float): ImageBitmap? =
        if (isEmpty) null else ImageBitmap(width, height).also { drawOnBitmap(it, penColor, penWidth) }

    internal companion object {
        val Empty: Signature = Signature(persistentListOf(), 0, 0)
    }
}
