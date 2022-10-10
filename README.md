# Signature Pad

## Gradle dependency

### Multiplatform

```kotlin
implementation("com.github.sproctor.SignaturePad:signaturepad:$signaturepad_version")
```

### Android

```kotlin
implementation("com.github.sproctor.SignaturePad:signaturepad-android:$signaturepad_version")
```

### JVM

```kotlin
implementation("com.github.sproctor.SignaturePad:signaturepad-jvm:$signaturepad_version")
```

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