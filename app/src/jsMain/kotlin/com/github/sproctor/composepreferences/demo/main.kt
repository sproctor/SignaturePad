package com.github.sproctor.composepreferences.demo

import androidx.compose.ui.window.Window
import com.github.sproctor.signaturedemo.SignatureBox
import org.jetbrains.skiko.wasm.onWasmReady

fun main() {
    onWasmReady {
        Window("Signature Pad Demo") {
            SignatureBox()
        }
    }
}
