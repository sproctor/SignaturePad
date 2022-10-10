# Signature Pad

## Gradle dependency

### Multiplatform

```kotlin
implementation("com.github.sproctor:SignaturePad:$signaturepad_version")
```

### Android

```kotlin
implementation("com.github.sproctor:SignaturePad-android:$signaturepad_version")
```

### JVM

```kotlin
implementation("com.github.sproctor:SignaturePad-jvm:$signaturepad_version")
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