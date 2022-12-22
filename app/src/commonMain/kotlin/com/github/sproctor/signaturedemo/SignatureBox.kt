package com.github.sproctor.signaturedemo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.seanproctor.signaturepad.SignaturePad
import com.seanproctor.signaturepad.rememberSignaturePadState

@Composable
fun SignatureBox() {
    var savedSignature: ImageBitmap? by remember { mutableStateOf(null) }
    val signaturePadState = rememberSignaturePadState(penColor = Color.Black)
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
                    enabled = enabled
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
                        text = if (enabled) "DISABLE" else "ENABLE"
                    )
                }
                Button(
                    onClick = { signaturePadState.clear() }
                ) {
                    Text("RESET")
                }
                Button(
                    onClick = {
                        savedSignature = signaturePadState
                            .getSignatureBitmap(600, 400)
                    }
                ) {
                    Text("Capture")
                }
            }
        }
    } else {
        Image(savedSignature!!, contentDescription = null)
    }
}