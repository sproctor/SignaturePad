# Compose Signature Pad

## Gradle dependency

### Multiplatform

```kotlin
implementation("com.seanproctor:signaturepad:$signaturepad_version")
```

As of 1.0.1, supported targets are Android, JVM, and JS (browser/canvas).

## Usage

```kotlin
val signaturePadState = rememberSignaturePadState(penColor = Color.Black)

SignaturePad(state = signaturePadState)

Button(
    onClick = {
        val bitmap = signaturePadState.getSignatureBitmap(600, 400)
        submitSignature(bitmap)
    },
) {
    Text("Submit")
}
```
