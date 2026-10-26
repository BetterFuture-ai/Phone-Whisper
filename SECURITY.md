# Security policy

Phone Whisper uses a user-enabled Android AccessibilityService to display a floating voice button alongside Gboard and insert dictated text into the active editor. Accessibility access can expose sensitive on-screen information; **only enable it if you understand and trust the installed application**.

## Download safety

Download releases only from this repository's **Releases** page. Verify the published SHA-256 checksum and do not disable phone security protections merely because an APK asks you to. Google Play Protect may block side-loaded AccessibilityService apps in some markets even when they are not known to be malicious. See [INSTALL.md](INSTALL.md) and [PLAY-PROTECT.md](PLAY-PROTECT.md) for details.

## What the application does

- Speech recognition runs locally on the phone using downloaded, hash-verified ONNX model files.
- The optional Hugging Face model-download token is encrypted through Android Keystore; it is not included in exported settings.
- Personal dictation history is opt-in. Audio is not retained after sessions; long temporary audio files are cleaned up.
- The companion is disabled on sensitive/password fields, and it checks editor identity before inserting text.
- The model download feature requires internet access, but speech recognition itself has no cloud fallback.

## Reporting a vulnerability

Please use GitHub's private vulnerability reporting option under the repository's **Security** tab if it is available. Otherwise, open an issue describing a non-sensitive bug **without sharing passwords, tokens, private speech, or exploit details**. The source is provided without warranty under the MIT License.
