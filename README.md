# Compose Signature Pad

## Gradle dependency

### Multiplatform

```kotlin
implementation("com.seanproctor:signaturepad:$signaturepad_version")
```

As of 1.0.1, supported targets are Android, JVM, and JS (experimental canvas).

## Usage

```kotlin
val signaturePadState = rememberSignaturePadState()

SignaturePad(state = signaturePadState, penColor = Color.Black, penWidth = 3.dp)

Button(
    onClick = {
        val bitmap = ImageBitmap(600, 400)
        signaturePadState.drawOnBitmap(penColor = Color.Black, penWidth = 2f)
        submitSignature(bitmap)
    },
) {
    Text("Submit")
}
```
