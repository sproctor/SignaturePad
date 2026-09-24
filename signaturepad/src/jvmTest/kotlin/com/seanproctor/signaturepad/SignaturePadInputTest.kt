package com.seanproctor.signaturepad

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SignaturePadInputTest {

    @Test
    fun swappingTheState_sendsLaterStrokesToTheNewState() = runComposeUiTest {
        val first = SignaturePadStateImpl()
        val second = SignaturePadStateImpl()
        var state: SignaturePadState by mutableStateOf(first)
        setContent {
            SignaturePad(state, Color.Black, 3.dp, Modifier.size(200.dp).testTag("pad"))
        }
        // Pointer input starts on the first event, so draw once before swapping.
        onNodeWithTag("pad").performTouchInput { swipe(Offset(20f, 60f), Offset(180f, 60f), 300) }

        state = second
        waitForIdle()
        onNodeWithTag("pad").performTouchInput { swipe(Offset(20f, 140f), Offset(180f, 140f), 300) }
        waitForIdle()

        assertTrue(second.signatureStarted.value, "the stroke went to the previous state")
    }
}
