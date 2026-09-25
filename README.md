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

### Other options

- `SignaturePad(enabled = false)` stops the pad from taking input.
- `signaturePadState.signatureStarted.value` becomes `true` once the user starts signing, which is handy for enabling a submit button.
- `signaturePadState.clear()` erases the pad and sets `signatureStarted` back to `false`.
