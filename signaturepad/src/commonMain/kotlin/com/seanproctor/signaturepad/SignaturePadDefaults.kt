package com.seanproctor.signaturepad

import androidx.compose.ui.unit.Dp

/** Default values used by [SignaturePad]. */
public object SignaturePadDefaults {
    /**
     * The default for [SignaturePad]'s `minPointDistance`. On desktop (JVM) it's 2.dp, because AWT
     * reports the mouse in whole pixels, and a slow stroke through every pixel it crosses comes out
     * as a wobbly staircase. On the other platforms, positions have sub-pixel precision, so every
     * move is drawn.
     */
    public val minPointDistance: Dp get() = platformMinPointDistance
}

internal expect val platformMinPointDistance: Dp
