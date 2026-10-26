# Goal and Progress.md

> **Project:** Phone Whisper — offline Android dictation with a Gboard companion
>
> **Status (26 October 2026):** Version **0.4.4** was built, signed, passed local speech-engine tests and was installed on the physical **OnePlus Nord 4 via ADB**. The user-reported v0.4.2 UX issues have been addressed in code. Normal OnePlus File Manager installation of v0.4.3 was **blocked by Play Protect** due to sensitive-access warnings; a v0.4.4 file-manager **update** prompted an app scan instead. This does **not** prove fresh installs on friends' phones will work. The real microphone/IME integration remains untested on v0.4.4 because the installed app reports the offline model is not present.

## 1. END GOAL

Build a practical, pleasant, private Android speech-to-text app for everyday phone typing. Keep the user's existing **Gboard** keyboard instead of replacing its swipe typing, predictions, clipboard, languages, and emoji with a basic custom keyboard. A small floating **Phone Whisper** voice control should appear while an editable text field and keyboard are active.

The app should:
- Recognize English speech **on the phone**, without uploading microphone recordings, relying on a subscription, or requiring an online transcription service.
- Make recording easy to start, finish and cancel, with no unnecessary preview or extra confirmation tap.
- Keep the main voice bubble and the contextual **X (discard, left)** and **✓ (finish/confirm, right)** aligned horizontally; let the user adjust their appearance and touch-friendly size in Settings.
- Finish recording with ✓, transcribe and **automatically paste into the same still-focused editor** when recognition completes; there is no review popup and no second ✓. X must discard without pasting, including while transcription is active.
- Support dragging and repositioning the voice bubble; dropping it onto the red dismiss target hides it until **Home → Show floating button** restores it.
- Offer useful local settings for model load/unload, dictation duration, processor use, corrections and snippets, backup/restore, local history, screen awake, and the optional basic keyboard.
- Deliver a correctly signed updateable APK that is responsive on the user's OnePlus Nord 4 and does not need heavy build/emulator processes on the user's Apple M1 MacBook Pro (8 GB RAM).

## 2. REQUIREMENTS

### Must have
1. **Gboard companion:** Android AccessibilityService / accessibility input-method integration provides a voice overlay without changing the selected keyboard. The overlay must not appear for sensitive/password fields.
2. **Single-action finish and safe insertion:** One ✓ finishes and automatically pastes the recognized result. X cancels/discards; editor/package/focus changes invalidate pending text. No review popup or second confirmation tap.
3. **Reliable local speech engine:** Fluid Speech Mini ONNX files are downloaded once (~300 MB), verified against the **pinned manifest** and then used offline; no automatic cloud fallback.
4. **Customizable controls:** Voice bubble size/opacity/icon/image/wave animation, recording mode, and action-control size/style; positioning must stay usable on different display bounds.
5. **Recoverable hidden button:** Drag-to-hide does not revoke accessibility permission and Home can re-enable the floating control.
6. **Usable app pages:** Home, Model, Words, History, Settings. History is opt-in; corrections/snippets can be managed and exported/imported.
7. **Long-session protections:** Time-limited and no-app-time-limit options; temporary cache files for longer audio, chunked transcription, storage/thermal safeguards and cleanup.
8. **Distribution:** Signed ARM64 APK for Android 13+ with the existing package ID `com.jitu.fluidspeech` and compatible signing key, allowing in-place upgrades.
9. **Privacy/security:** Avoid retaining long-term audio; verify download hashes; use HTTPS for alternative model sources; encrypt optional Hugging Face access token using Android Keystore and do not export it in backups. General-purpose AccessibilityService access requires a prominent, affirmative in-app disclosure before directing the user to Android permission settings.

### Should have
- Button placement not obstructing app send/edit controls; friendly information and feedback during recording, transcription and review.
- A compact performance panel for measured processing modes, clearly identifying estimates, device specificity, and unmeasured battery usage.
- Practical recovery messages for model/network issues and a documented safe install method.
- Tested preference persistence and correctness after navigating between screens and changing orientation, keyboard, focus and recording state where applicable.

### Constraints
- **Mac:** MacBook Pro 13-inch, Apple M1, 8 GB RAM; preserve responsiveness. Prefer low-priority, bounded JDK/build tool runs. Avoid heavyweight emulators, Gradle daemons, unnecessary global installs, and high-CPU background processes.
- **Phone:** OnePlus Nord 4, Android 13+ supported; real-device results supersede simulated UI inspection.
- **Do not remove or overwrite:** model files, personal signing key `.deps/personal-release.jks`, settings, or the working app install without a specific justified reason.
- **Version truthfulness:** A passing compiler, offline inference test, or APK signature check does not prove a new Android overlay workflow works on a phone.
- **Git:** User prefers **no unnecessary checkpoints**. Do not add checkpoints merely to copy/update the project documentation.
- Do not send audio, saved transcripts, or correction rules to a third-party backend.
- Keep changes focused on the problem and do not discard existing uncommitted source changes.

### Quality standards
- No unintentional text insertion; cancellation must be trustworthy.
- Resilient to Android accessibility/IME events, focus changes, overlay-window updates, service teardown, and denied permission.
- Privacy by default, safe file handling, validated downloads, limited memory and CPU, accurate release documentation.
- Verification must state what was tested on Mac versus a physical phone.

## 3. SUCCESS CRITERIA

| Requirement | Expected result | Verification evidence |
| --- | --- | --- |
| Install/update | v0.4.4 installs for developer testing; updates retain package identity. Friend-facing installation must comply with Play Protect | ADB install confirmed v0.4.4 (versionCode 8); normal file-manager fresh install of v0.4.3 blocked; formal distribution issue unresolved |
| Dictation | Clear English recording transcribes locally and produces intelligible text | Record several real phrases with network off, compare transcript |
| Cancel | During recording, pressing X immediately stops; no words appear in target editor | Record, press X, inspect editor and clipboard/history effects |
| One-tap ✓ | A single ✓ finishes, then automatically pastes recognized text into the original active field with no preview | Record on physical phone with installed speech model; check insertion, cancellation and changed-field rejection |
| Focus safety | Switching text fields, closing keyboard, or editing a password field invalidates pending dictation | Perform each negative-path test and verify nothing is pasted in a different field |
| Controls | Visible ✕ / bubble / ✓ stay touchable and in a row; configurable size/style work | Settings preview symbols observed on Nord 4; live keyboard overlay still needs testing across positions/styles and input apps |
| Hide and restore | Drag bubble to red target → hidden; Home → Show floating button → visible again | Test live, restart the app and re-open Gboard |
| Model integrity | Exact required files hash/size check; HTTPS mirror cannot bypass checks | Hash/manifest inspection and real download/resume only if appropriate |
| Long recording | A 2-minute sample and manual stop in unlimited mode complete without undue memory, heat, or data loss | Physical phone trials with RAM/thermal observation; verify temp file cleanup |
| Package quality | APK compiles, signature validates, engine/rules tests pass | Run `tools/build.py`, `tools/test-engine.sh`, signature/package inspection |
| Mac impact | Builds remain bounded; Mac stays usable | Check process RAM/CPU during actual build or reproduction |

**Completion gate:** The project is not fully verified until real-phone acceptance tests pass. User feedback can change priorities; capture concrete symptoms and retest the original failure before declaring success.

## 4. APPROACH

### Architecture
- **UI:** `app/src/com/jitu/fluidspeech/MainActivity.java` and `Ui.java` build the native Android UI.
- **Companion:** `CompanionService.java` is the AccessibilityService overlay/controller; `BubbleView.java`, `BubbleGestures.java`, and `OverlayControls.java` manage the floating control/gestures/review actions. Tracks `fieldVersion`, input package, and sensitive fields.
- **Speech:** `SpeechController.java` owns microphone capture, asynchronous model loading/inference, cancellation and idle unload. `AsrEngine.java` wraps the offline ONNX inference.
- **Models:** `ModelStore.java` validates/downloads the pinned ONNX assets using the manifest `app/assets/model-manifest.json`. `ModelTokenStore.java` encrypts the optional download token using Android Keystore.
- **Settings/text:** `AppPrefs.java` holds app preferences, `RuleStore.java` and `TextRules.java` process correction rules and snippets.
- **Other keyboard:** `VoiceKeyboard.java` is an optional basic alternative; it should not be required for Gboard companion mode.
- **Tests/build:** `tests/` has Java inference and text rules tests. `tools/build.py` uses JDK 17, Android SDK platform/build tools 35, a bounded Java heap, and existing project-local dependencies; no Gradle daemon.
- **Distribution:** `Install/PhoneWhisper.apk` is the sole current installer. Read `Install/READ-ME-FIRST.txt` before installation.

### Development strategy
1. Observe the live state and reproduce a reported defect.
2. Locate the owning subsystem and check dependent paths, including cancellation and cleanup.
3. Make the smallest supported change, then compile and run focused tests.
4. Validate on the phone when the behavior depends on Android UI/accessibility/microphone.
5. Update **this file**, `Memory.md`, and `PSD.md` for significant changes, real testing evidence, or important design decisions.

## 5. MILESTONES

1. Foundation: offline engine, Android build/signing setup, project-local model and resources.
2. Core dictation: model load/unload, recording, corrections/snippets, basic UI.
3. Gboard companion: accessibility-based overlay, positioning and text insertion.
4. Usability/safety: confirmation controls, hide/restore, backup and settings, long audio safeguards.
5. Validation: on-device UI/typing/focus tests, performance, battery/heat as applicable, installation verification.
6. Release-ready handoff: stable signed APK, documentation of limits and acceptance checks.

## 6. COMPLETED — VERIFIED EVIDENCE

- The original 0.4.1 app was **installed and launched** on the OnePlus Nord 4 in prior testing. Its short inference benchmark produced correct text for a public 11-second sample with 1/2/4/6/8 CPU-thread options. Timings and PSS are in `README.md`, and battery usage is explicitly unmeasured.
- Android package `com.jitu.fluidspeech` has been maintained; the current v0.4.4 manifest specifies versionCode 8, versionName 0.4.4 and minimum SDK 33.
- v0.4.4 implements **visible** X/✓ glyphs, single-✓ stop and automatic paste, bubble drag-to-hide/Home recovery, customizable controls, a Model-card gear that opens an X-close configuration popup, verified custom HTTPS source/encrypted token, and an accessibility disclosure with explicit consent.
- On 26 October 2026, v0.4.4 was built with `nice -n 19 python3 tools/build.py`; signed APK v3 verification passed, `tools/test-engine.sh` passed inference/stale-task/cancel/reuse tests and `git diff --check` passed.
- OnePlus Nord 4 was connected by USB. Fresh v0.4.3 installation through OnePlus File Manager triggered Google's **“App blocked to protect your device”** warning referencing sensitive data access. An update of v0.4.4 from File Manager prompted **“App scan recommended”** instead; no APK was submitted to Google's scanner. v0.4.4 was then installed successfully via ADB without disabling protection, and package manager reported versionCode 8/versionName 0.4.4. Model gear/popup and X-close, visible X/✓ preview labels, and the new consent dialog were inspected via Android UI hierarchy. Recording-to-text could not be tested without installing the missing model. No unnecessary Git checkpoint was created.
- Documentation in `README.md` and `Install/READ-ME-FIRST.txt` describes the new release and test steps.

## 7. REMAINING — NOT YET VERIFIED

1. **Full physical-phone test of v0.4.4 after model readiness.** Verify one-tap ✓ automatic paste, X cancellation, no preview popup, focus changes, drag-to-hide/restore and action glyph appearance. Current installation reports SETUP NEEDED; don't silently download ~300 MB without confirming network preference.
2. **Friend-facing distribution (confirmed platform restriction):** Google's Enhanced Fraud Protection in India automatically blocks File Manager/browser installs of apps declaring AccessibilityService, with no normal Install anyway action. Official Play distribution is preferred, or consider an official Play Protect classification appeal (not guaranteed). See `PLAY-PROTECT.md` for exact observations, appeal draft and links. As of 31 August 2026, **new Google Play apps and updates require target SDK 36**, while this project still targets 34 and has local SDK 35. A careful API36 upgrade and Play Console/accessibility declaration are required before submitting. The owner must provide account/appeal authority. Never disable or disguise protection.
3. Longer recordings (2 minutes and unlimited mode), temp file cleanup, and sustained thermal/battery performance need real-device validation.
4. Optional paths such as alternate model download source, token use and backup import/export should be tested on device if required for release confidence.
5. Perform a final fresh, requirement-by-requirement real-device acceptance check before labeling the release fully tested.

## 8. NEXT

- **Immediate external dependencies:** Receive on-device dictation test after the model is ready and choose a legitimate friend-facing distribution route (reviewed Google Play build or Play Protect classification appeal). Refer to `PLAY-PROTECT.md`; if choosing Google Play, update/test to API 36 without disturbing the currently installed personal build. Capture exact symptom, screen and active editor context.
- **Meanwhile:** Keep the current working APK and existing uncommitted source changes intact. Documentation housekeeping is safe; do not quietly change untested Android UI code.
- If a defect is reported: inspect current source and actual logs, reproduce, patch narrowly, run the lightweight build/tests, and arrange real-phone retest for the affected behavior.
- If tests pass: update Completed and the success-criteria evidence, document any residual risks, and only then consider further feature work.

## 9. GOAL EVOLUTION

- Initial product aimed for fast, local Android voice typing while keeping Gboard.
- Later usability requirements added **X/discard and ✓/review/confirm**; the user's v0.4.2 test then explicitly removed review/second confirmation. The accepted v0.4.4 behavior is **single ✓ to finish and automatically paste**, or X to cancel, with active-editor safety checks.
- Current release work added verified alternate model download sources and optional token storage, while preserving the pinned model and offline inference model.
- These new requirements must not remove or weaken existing privacy, recording, backup, or update compatibility.

## 10. FINAL CHECK

Before declaring this app complete:
1. Read this document's END GOAL and all mandatory requirements.
2. Inspect the actual source, APK, signing metadata and live device behavior.
3. Test **negative paths**: X, empty audio, another text field, keyboard dismissal, password field, and failed insertion.
4. Check related features and performance after fixes.
5. Record **exact evidence and any remaining unknowns** here; never turn an unverified test into a completed milestone.

---

**Principle:** Goal = destination; Progress = reality; Remaining = known gaps; Next = next practical action; Success criteria = observable evidence.
