package com.seanproctor.signaturedemo

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import org.jetbrains.skiko.wasm.onWasmReady

@Suppress("DEPRECATION_ERROR")
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    onWasmReady {
        @Suppress("DEPRECATION_ERROR")
        CanvasBasedWindow("Signature Pad Demo") {
            SignatureBox()
        }
    }
}
