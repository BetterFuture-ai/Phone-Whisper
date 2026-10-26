# Contributing to Phone Whisper

Thanks for helping improve offline, accessible-by-choice Android voice typing.

## Before you start

- Read [README.md](README.md), [INSTALL.md](INSTALL.md) and [SECURITY.md](SECURITY.md), especially the Android AccessibilityService trust implications.
- Check existing GitHub issues before starting major changes.
- Keep pull requests small and focused. Describe what changed, why, and **how it was verified**.
- Include actual physical-device behavior when changing the Accessibility overlay, microphone, focus tracking or keyboard insertion.
- Do not change application ID, signing identity, pinned model hashes or accessibility-tool classification without a detailed rationale.

## Development

The project uses Java, Android SDK build tools, ONNX Runtime and a lightweight Python builder. It has no required Gradle daemon.

```bash
python3 tools/prepare.py
python3 tools/build.py
bash tools/test-engine.sh
```

Scripts may download Android/ONNX dependencies and model validation files. Refer to the README for resource and platform requirements. The local keystore and signing config reside in `.deps/` and are never committed.

## Pull request checklist

- [ ] Tested `tools/test-engine.sh` where applicable
- [ ] APK builds and signature verification succeeds, if Android source changed
- [ ] Real-device UI/IME/Accessibility behavior tested if affected
- [ ] No credentials, signing keys, model binaries, private records, audio samples or local machine paths committed
- [ ] Documentation, privacy disclosures and relevant source index updated
- [ ] Regression and failure cases explained, including limitations

## Code style

Prefer a small, understandable fix over adding heavy libraries. Keep microphone/audio inference work off the UI thread, avoid repeatedly changing overlay window positions, and reject stale dictation if editor context changes.

## Issue reports

Include Android version, device, app version, exact steps, expected behavior and actual behavior. Do not publish private transcripts or sensitive screenshots. For security-sensitive issues, follow [SECURITY.md](SECURITY.md).

## License

By contributing, you agree your original contributions may be distributed under the project's [MIT License](LICENSE), while third-party code and model assets continue under their respective licenses.
