# Phone Whisper v0.4.4 — first open-source release

**Speak freely. Keep Gboard.**

Phone Whisper is a lightweight Android dictation companion that works with your existing Gboard keyboard. The app records when you ask, transcribes speech with an on-device ONNX model, and inserts the result into your focused text field.

## What's included

- Native Home, Model, Words, History and Settings pages.
- Movable, customizable floating microphone with visible **✕ / ✓** action buttons.
- One ✓ to finish and auto-paste; X immediately discards without typing.
- Accessibility-based insertion safeguards for field changes and password inputs.
- A configurable, SHA-256-verified ~300 MB speech model download.
- Optional encrypted Hugging Face token and custom HTTPS model source.
- Local word corrections, spoken snippets and optional transcript history.
- Lightweight Python + JDK 17 Android build, tests and dependency preparation.
- MIT-licensed original application code, upstream ONNX/model license notices.

## Compatibility

- Android 13+ (API 33), ARM64.
- Offline English speech recognition after first-time model download.
- Package: `com.jitu.fluidspeech`, versionCode 8.
- Release asset: **`PhoneWhisper.apk`** (~12.5 MiB).
- SHA-256: `18d37b85d414a9468c11e0c17c8f0ebc64418c5626e2458853c76044ad608c30`.

## Important installation warning

Google Play Protect may block direct APK installation in some places (confirmed on OnePlus Nord 4 in India) because the Gboard companion requires an AccessibilityService. **No “Install anyway” action may be provided.** Android Debug Bridge installation succeeded for developer tests; that does **not** mean Play Protect approves ordinary sideloads. Read the [installation guide](INSTALL.md) and [security policy](SECURITY.md).

## Verification status

- Build, v3 signature validation and on-device model inference smoke tests passed.
- Basic settings, the model source popup and Accessibility disclosure were inspected on OnePlus Nord 4.
- The v0.4.4 complete record-to-paste experience, focus-change regression and sustained long-recording behavior still need end-to-end phone testing. Contributors welcome.

## Credits

See [README.md](README.md) and [app/assets](app/assets) for FluidVoice-Labs model and ONNX Runtime attribution.
