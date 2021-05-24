package com.github.sproctor.signaturedemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.sproctor.signaturedemo.ui.theme.SignaturePadDemoTheme
import com.github.sproctor.signaturepad.SignaturePad
import com.github.sproctor.signaturepad.rememberSignaturePadState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignaturePadDemoTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    SignatureBox()
                }
            }
        }
    }
}

@Composable
fun SignatureBox() {
    val signaturePadState = rememberSignaturePadState(penColor = Color.Black)
    Column {
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .aspectRatio(1.5f),
        ) {
            SignaturePad(
                modifier = Modifier
                    .background(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                state = signaturePadState,
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
        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = { signaturePadState.clear() }
        ) {
            Text("RESET")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SignaturePadDemoTheme {
        SignatureBox()
    }
}