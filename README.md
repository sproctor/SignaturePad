# Compose Signature Pad

## Gradle dependency

### Multiplatform

```kotlin
implementation("com.seanproctor:signaturepad:$signaturepad_version")
```

Supported targets are Android, JVM (desktop), JS, Wasm, and iOS.

## Usage

```kotlin
val signaturePadState = rememberSignaturePadState()

SignaturePad(state = signaturePadState, penColor = Color.Black, penWidth = 3.dp)

Button(
    onClick = {
        val bitmap = ImageBitmap(600, 400)
        signaturePadState.drawOnBitmap(bitmap, penColor = Color.Black, penWidth = 2f)
        submitSignature(bitmap)
    },
) {
    Text("Submit")
}
```

### Keeping the signature

`rememberSignaturePadState()` keeps the signature only while the pad stays in the composition. To also keep it when the activity is recreated on Android (after a configuration change or process death), use `rememberSaveableSignaturePadState()` instead.

### Resizing

By default the signature is cleared whenever the pad changes size. This can happen on rotation, or when the keyboard opens and makes the pad smaller. Pass a `ResizeBehavior` to keep it:

```kotlin
val signaturePadState = rememberSaveableSignaturePadState(ResizeBehavior.Fit)
```

- `Clear` erases the signature. This is the default.
- `Center` keeps its size and moves it to stay centered.
- `Fit` scales it to fit the new size without distorting it.
- `Stretch` scales each axis to fill the new size.
- `Custom` maps each point with your own function.

### Saving the signature as it's drawn

`signaturePadState.signature` holds the finished strokes as an immutable `Signature`, and gets a new one each time a stroke ends or the signature is cleared, resized or restored. Watch it to save the signature without a submit button:

```kotlin
LaunchedEffect(signaturePadState) {
    snapshotFlow { signaturePadState.signature }
        .collectLatest { signature ->
            if (!signature.isEmpty) {
                val bitmap = withContext(Dispatchers.Default) {
                    signature.toImageBitmap(Color.Black, penWidth = 6f)
                }
                bitmap?.let { saveSignature(it) }
            }
        }
}
```

The check skips an empty signature. A new pad starts with one, so opening the screen doesn't overwrite a signature you saved before. Clearing the pad also gives an empty signature, so if clearing should delete the saved one too, do that where you call `clear()`.

A `Signature` never changes, so it can be exported on a background thread, as here. `toImageBitmap` draws it at the pad's size (`width` by `height`), or returns `null` when it's empty, and `drawOnBitmap` scales it to fit a bitmap of your own.

### Other options

- `SignaturePad(enabled = false)` stops the pad from taking input.
- `SignaturePad(minPointDistance = 6.dp)` skips moves closer than that to the last point drawn. The default, `SignaturePadDefaults.minPointDistance`, is 4.dp on desktop, where the mouse only reports whole pixels and slow strokes would come out wobbly, and 0.dp on the other platforms.
- `signaturePadState.signatureStarted.value` becomes `true` once the user starts signing, which is handy for enabling a submit button.
- `signaturePadState.clear()` erases the pad and sets `signatureStarted` back to `false`.
- The state can be used from any thread, so you can clear, resize or export from a background thread while the pad is drawing. Input you feed it yourself goes into the same stroke as the user's, so don't send it while the user is drawing.
