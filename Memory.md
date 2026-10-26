# Memory.md

> **Quick project context — 26 October 2026.** Read this first, then consult `Goal and Progress.md` for detailed goals/status, `PSD.md` for past problem-solving decisions, and `AGENTS.md` for working rules.

## PROJECT CONTEXT

**Phone Whisper** is a personal offline English dictation app for **Android 13+**, developed primarily for a **OnePlus Nord 4**. It keeps **Gboard** selected and uses an accessibility companion to show a movable voice bubble while a text field/keyboard is active. It also has an optional simple keyboard.

Current deliverable: **v0.4.4** at `Install/PhoneWhisper.apk`, Android package `com.jitu.fluidspeech` (versionCode 8). It built, signed, passed the local engine tests and **installed by ADB** on the Nord 4. The phone UI showed the model-source popup, X/✓ action preview and Accessibility consent dialog. A new model is **not installed in the current app instance** (Home shows SETUP NEEDED), so live recording/auto-paste cannot yet be verified.

**Important requirement:** The floating action row is **X · voice bubble · ✓**. X cancels without typing; **one ✓ finishes recording and automatically pastes when transcription completes, with no preview/second click**. Pending text must be rejected if focus/editor context changed. Drag-to-hide uses a red zone; Home restores the bubble. Settings allows action size/style changes.

## CURRENT UNDERSTANDING

- `MainActivity.java` renders Home, Model, Words, History and Settings; `Ui.java` provides shared native Android components.
- `CompanionService.java` handles the Gboard accessibility overlay, editor identity checks, recording state, confirm/discard and insertion.
- `BubbleView.java`, `BubbleGestures.java` and `OverlayControls.java` implement the draggable voice icon and its side controls.
- `SpeechController.java` serializes model work, records audio, handles stop/cancel and forwards results; `AsrEngine.java` runs Fluid Speech Mini with ONNX Runtime **on the phone**.
- `ModelStore.java` downloads **manifest-pinned** model files (~300 MB) and validates sizes/SHA-256. Custom HTTPS mirror allowed; optional Hugging Face token is kept encrypted in Android Keystore through `ModelTokenStore.java`. No cloud transcription.
- `AppPrefs.java`, `RuleStore.java` and `TextRules.java` manage settings, corrections and snippets. History defaults off; long audio uses temporary, cleaned cache files.
- The model and its settings remain in Android private app storage. Do not change package ID, signing key or model directory casually.

## SYSTEM KNOWLEDGE

- Project is opened at the repository root, containing `app/`, `tools/`, `tests/`, `Screenshots/` and a Git-ignored `Install/` with the locally built APK. Use relative paths to keep public documentation portable.
- Current installer: `Install/PhoneWhisper.apk`. See `Install/READ-ME-FIRST.txt`; only deliver the single current installer.
- Low-overhead local build: `nice -n 19 python3 tools/build.py`; engine/regression tests: `bash tools/test-engine.sh`. Uses JDK 17, Android SDK 35 and dependencies under `.deps/`; **no Gradle daemon required**.
- The original personal signing key is `.deps/personal-release.jks`. Keep it protected; losing or replacing it can break in-place upgrades.
- `README.md` contains features/tests; `PLAY-PROTECT.md` contains **confirmed India Play Protect hard-block root cause**, current Android permission audit, non-bypass remedies, API36 Play distribution prerequisite, and an unsubmitted appeal draft.
- Development Mac: **13-inch M1 MacBook Pro, 8 GB RAM**. Favor short, low-resource tool runs, avoid an unnecessary emulator and minimize CPU/RAM disruption.
- The working tree contains **uncommitted changes** through v0.4.4, including the four project-context Markdown files. Do not reset them; user does not want pointless Git checkpoints.

## EVOLVING LEARNINGS

- A clean APK build and successful desktop inference do **not** demonstrate correct Android overlay behavior. Real-phone check is required.
- Overlay window updates can themselves cause accessibility callbacks. Avoid repeatedly updating unchanged overlay layout coordinates.
- The accessibility input connection's `commitText` returns **void**; do not test it as a boolean.
- The user rejected a review popup and extra ✓ click. Finishing with ✓ or the recording control should auto-paste the recognized words only into the original active editor; X must suppress pasting. Preserve field-version/package checks.
- 1/2/4/6/8 CPU modes showed different timings on one short sample; 8 cores did not beat 6. Don't advertise unmeasured battery gains.
- v0.4.4 build/signing/engine tests passed and installed via ADB. Fresh File Manager v0.4.3 install was **Play Protect hard-blocked**; File Manager update to v0.4.4 prompted a distinct app scan, not consumer-install approval. This is Google's India Enhanced Fraud Protection for **internet-sideloaded AccessibilityService apps**, not an APK compilation error. Friend-facing Play distribution requires **API36 target** (current target34, installed SDK35). No app scan, VirusTotal upload, Play Protect appeal or Google Play publication was submitted. Full voice/long-session acceptance remains pending.

## SYSTEM IMPROVEMENTS

- Added customizable, now-visible X/✓ controls with **single-action paste**, drag-to-hide/restore, a gear-triggered model configuration dialog, optional verified mirror/token and a separate Accessibility disclosure with affirmative consent; package v0.4.4.
- Corrected potential accessibility-overlay layout-event feedback by updating position only when changed. The earlier action glyphs were obscured by an elevated background, so their elevation/layering was fixed.
- Adopted the root-level four-file project continuity system: `AGENTS.md`, `Goal and Progress.md`, `Memory.md`, and `PSD.md`.

## MAINTENANCE RULES

Keep this page **short, current, factual**. Update it only for major discoveries or milestones. Put acceptance criteria and detailed progress into `Goal and Progress.md`; put failed approaches and engineering decisions into `PSD.md`; put general work rules into `AGENTS.md`. Continue recording concrete physical-device evidence. A live X/✓-to-text test requires a ready speech model; do not treat an ADB install as proof that Play Protect allows normal distribution.

**Memory principle:** Read this file → grasp the project quickly → inspect only the relevant code and detailed files.
