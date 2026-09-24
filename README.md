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
