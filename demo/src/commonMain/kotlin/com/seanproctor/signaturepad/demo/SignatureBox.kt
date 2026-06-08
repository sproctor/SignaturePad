package com.seanproctor.signaturepad.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.seanproctor.signaturepad.ResizeBehavior
import com.seanproctor.signaturepad.SignaturePad
import com.seanproctor.signaturepad.rememberSaveableSignaturePadState

@Composable
fun SignatureBox() {
    var savedSignature: ImageBitmap? by remember { mutableStateOf(null) }
    val signaturePadState = rememberSaveableSignaturePadState(ResizeBehavior.Fit)
    var enabled by remember { mutableStateOf(true) }
    if (savedSignature == null) {
        Column {
            Card(
                modifier = Modifier
                    .weight(1.0f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .aspectRatio(1.5f),
            ) {
                SignaturePad(
                    modifier = Modifier
                        .background(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                    state = signaturePadState,
                    enabled = enabled,
                    penColor = Color.Black,
                    penWidth = 3.dp
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Divider(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        text = "Sign above the line",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = { enabled = !enabled }
                ) {
                    Text(
                        text = if (enabled) "Disable" else "Enable"
                    )
                }
                Button(
                    onClick = { signaturePadState.clear() }
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = {
                        savedSignature = ImageBitmap(600, 400)
                        signaturePadState.drawOnBitmap(savedSignature!!, Color.Black, 2f)
                    },
                    enabled = signaturePadState.signatureStarted.value
                ) {
                    Text("Capture")
                }
            }
        }
    } else {
        Image(savedSignature!!, contentDescription = null)
    }
}