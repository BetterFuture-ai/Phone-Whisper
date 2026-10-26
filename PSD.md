# PSD.md

# Problems, Solutions and Decisions — Phone Whisper

> **State:** Updated 26 October 2026. These are actual observations, implementation decisions, and known verification gaps. Avoid presenting untested Android behavior as proven.

## 1. PROBLEMS and Solutions

### 1. Accidental insertion from floating dictation
- **Problem:** A floating dictation tool can paste unwanted words in the middle of an essay or another focused field when users change their mind.
- **Cause/impact:** Automatic insertion after speech recognition is unsafe when users cancel, navigate, or change input targets.
- **Decision/implemented mitigation:** At the user's explicit request, v0.4.4 changed to **single-action finishing**: X discards; ✓ stops recording and automatically pastes the result. There is no review preview or second tap. `CompanionService` still records field version and app/package, rejects mismatches, and avoids sensitive/password fields.
- **Verification:** v0.4.4 compiles and the phone exposes X/✓ glyphs in Settings preview; actual speech-to-editor insertion remains untested because the fresh installation lacks downloaded model files.
- **Lesson:** Treat preventing unintended typing as a hard functional requirement, not just a decorative UX improvement.

### 2. Repeated overlay layout updates can cause callback churn
- **Problem:** Android accessibility window changes may include events caused by the app's own overlay.
- **Cause/impact:** Updating X/✓ overlay position unconditionally on every refresh may repeatedly trigger more refreshes and waste CPU or harm responsiveness.
- **Approach:** Reposition windows **only when x/y actually changed**. A previous design updated even unchanged control positions.
- **Result:** Source corrected; v0.4.4 builds successfully. Live frequency/CPU measurement is **not yet available**.
- **Lesson:** Keep overlay refresh idempotent and avoid unnecessary `WindowManager.updateViewLayout` calls.

### 3. Incorrect assumption about Android accessibility commit API
- **Problem:** An attempted safety check caused Java compilation to fail with `'void' type not allowed here`.
- **Cause:** `AccessibilityInputConnection.commitText(...)` returns `void`, unlike related input-connection APIs that may return booleans.
- **Tried:** `if (!ic.commitText(...))` → failed during the Java build.
- **Worked:** Restore the API-correct `ic.commitText(...)` call; rebuilt and reran speech tests successfully.
- **Lesson:** Verify the exact Android framework return signature before adding error checks; static compilation catches API mismatch but cannot prove actual insertion.

### 4. Offline model download availability and integrity
- **Problem:** Model users may need an alternative when the default Hugging Face source cannot be accessed; a token is not a universal fix for outages.
- **Approach:** Keep the pinned file manifest as the trust source; allow a validated **HTTPS model folder URL**, optional Hugging Face read-only token, limited redirects, pinned size and SHA-256 checks. Token stored using Android Keystore, not in settings backups.
- **Why:** An alternate transport can be useful without changing the model identity or allowing unaudited weights.
- **Status:** Implemented and compiled. A full alternate-source download/token/redirect test on the phone remains outstanding.
- **Lesson:** Changing the download host must never bypass the pinned integrity checks; never forward the Hugging Face token to arbitrary mirror hosts.

### 5. Physical-phone verification and missing speech model
- **Problem:** A local build does not prove the Accessibility/IME UI works; a fresh installation can have no speech model.
- **Evidence:** The Nord 4 connected via USB. v0.4.4 installed successfully with `adb install -r` and Android reported versionCode 8. On-device UI hierarchy showed gear-triggered model configuration popup with X close, visible X/✓ labels in Settings, and the explicit Accessibility-access disclosure. The app currently displays SETUP NEEDED: model files must be downloaded again before live dictation verification.
- **Not a solution:** Assuming glyph presence in UI hierarchy proves overlay recording or that compiling proves typing works.
- **Next:** On-device dictation, cancellation and focus-safety checks when the model is ready.

### 6. Mac resource budget
- **Problem:** Heavy emulation/build tools risk slowing an M1 Mac with 8 GB RAM.
- **Approach:** Native low-memory Java/Python Android build without a Gradle daemon, using existing JDK 17, Android SDK and local project dependencies; favor `nice -n 19` and short tool batches.
- **Evidence:** v0.4.4 native build and desktop speech tests completed. The desktop test reported under ~600 MB maximum resident memory in its run; this is *not* a device-wide or full-build memory certification.
- **Lesson:** Keep new tests/tooling lightweight. Start an emulator only when the benefit justifies its cost and resource safeguards exist.

### 7. Floating X and ✓ appeared as solid color circles
- **Problem:** The Nord 4 showed colored left/right circles without visible X and check glyphs.
- **Cause:** Each circle background had `View.setElevation(3dp)`, while the custom glyph overlay had no elevation, allowing Android to draw the circle over its child icon.
- **Worked:** In `OverlayControls.button`, use high-contrast TextView glyphs ✕ and ✓ with greater elevation than the circle; v0.4.4 Settings UI hierarchy exposes both glyphs.
- **Pending:** Inspect live floating overlay while recording after the model is ready, including different action sizes/styles.
- **Lesson:** Child draw order does not override elevated sibling z-order. Validate actual rendering of controls on the target device.

### 8. Play Protect blocked ordinary APK installation
- **Problem:** When the signed Phone Whisper 0.4.3 APK was opened from the OnePlus File Manager on a USB-connected Nord 4, Google Play Protect showed **“App blocked to protect your device”**, citing access to sensitive data and identity-theft/financial-fraud risks. Android did not install it.
- **Cause supported by Google guidance:** The app declares an `AccessibilityService` to retain Gboard and insert dictated text. Google says internet/file-manager-sideloaded apps with Accessibility access can be blocked in supported markets. The warning did **not** identify an APK ZIP/signature error, and is not proof of malware. Its manifest accurately marks `isAccessibilityTool=false`.
- **Tried:** Signed/verified APK, ordinary user File Manager installation → Play Protect blocked fresh install; v0.4.4 File Manager update over an ADB-installed v0.4.3 → **“App scan recommended”**, not an automatic consumer-install success. **No APK was uploaded for a Google scan.**
- **Developer test:** `adb install -r Install/PhoneWhisper.apk` installed v0.4.4 successfully with protection settings unchanged. **ADB installation is not a consumer distribution solution.**
- **Legitimate next path:** Follow Google's [Developer Guidance for Google Play Protect Warnings](https://developers.google.com/android/play-protect/warning-dev-guidance), request classification review through the [Play Protect Appeals Submission Form](https://support.google.com/googleplay/android-developer/contact/protectappeals) when appropriate, or release a properly reviewed build through Google Play. The form requires owner email, package and **SHA-256 of an APK uploaded to VirusTotal**; never upload private app code externally or submit an appeal without the owner's approval.
- **Compliance improvement:** v0.4.4 now provides a separate prominent Accessibility disclosure with explicit affirmative consent; `isAccessibilityTool` remains correctly false. Google Play additionally requires an Accessibility declaration and review. [Official policy](https://support.google.com/googleplay/android-developer/answer/10964491).
- **Additional official finding (26 October 2026):** Google says this specific *App blocked to protect your device* warning is an automatic internet-sideloading restriction for apps declaring Accessibility (including India); unlike the separate *App scan recommended* warning, the hard-block screen has no normal Install anyway route. A classification appeal may be requested, but approval is not guaranteed. For a new Play release, the project must move from target API34 to API36 before submission under the August 2026 Play policy. Full findings and the owner-review appeal draft are in `PLAY-PROTECT.md`.
- **Lesson:** A signed APK and a successful ADB install do not prevent source-based Play Protect blocks. Do not weaken Play Protect or mislabel the app to hide Accessibility usage.

## 2. DECISIONS

### Decision 1 — Preserve Gboard as the default keyboard
**Reason:** Keep established swipe typing, predictions, emoji, languages and clipboard while adding an optional voice button.

**Alternative:** Replace Gboard with a custom keyboard. A simple optional `VoiceKeyboard` exists, but it is not feature-equivalent.

**Future implication:** All new primary dictation features must work with the AccessibilityService overlay and its keyboard/focus lifecycle.

### Decision 2 — Local model and bounded processing
**Reason:** Privacy, offline use and user control. Fluid Speech Mini runs via ONNX; preset processing choices are 1/2/4/6/8 threads; long recordings use temporary disk-backed PCM and 30-second inference portions.

**Alternative:** Cloud recognition or retaining full-length audio buffers in RAM. Neither is the default plan.

**Future implication:** Guard thermal/storage/memory conditions; clean temporary audio; preserve attribution and pinned manifest; do not promise sustained speed/battery without tests.

### Decision 3 — Single ✓ finishes and automatically pastes
**Reason:** The user tested the previous two-step review and explicitly rejected the extra tap and visible preview. Faster dictation is now mandatory.

**Alternative:** Two-stage ✓ confirmation with transcript preview (implemented in v0.4.2, rejected by user).

**Future implication:** X must reliably discard and ✓ must finish; successful transcription pastes automatically into the original editor, not another field. Preserve field-version/package checks. Test field switches, keyboard close, password fields, empty result, cancellation and service shutdown.

### Decision 4 — Preserve installed app data and signing identity
**Reason:** User should be able to update without downloading the large model again or losing preferences.

**Approach:** Keep package ID `com.jitu.fluidspeech`, private files directory and existing `.deps/personal-release.jks` key. v0.4.4 is versionCode 8.

**Future implication:** Treat a package rename, signing-key replacement, or destructive storage migration as a high-risk change requiring justification and direct verification.

### Decision 5 — Use project-local tools and minimize checkpoints
**Reason:** The user wants a responsive Mac and explicitly requested **no unnecessary Git checkpoints**.

**Approach:** Reuse `tools/build.py` / `tools/test-engine.sh`, do not add a Gradle/Docker setup or create Git checkpoints for routine documentation.

**Future implication:** Inspect `git status` before changing source; preserve uncommitted v0.4.4 work and write only necessary files.

### Decision 6 — Four project continuity documents in repository root
**Reason:** The user maintains a standard agent-working structure at `Documents/IMP MD Files/Project specific`.

**Approach:** Keep `AGENTS.md` for work rules, `Goal and Progress.md` for full objective/current status, `Memory.md` for compressed context and `PSD.md` for decisions and lessons.

**Future implication:** Update the right file after meaningful work, avoid bloating the short memory file, and do not replace evidence with status assumptions.

### Decision 7 — Respect Play Protect while preparing friend-facing distribution
**Reason:** The normal file-manager installer was actually blocked by Google's sensitive-permission policy; the user wants nontechnical friends to install safely.

**Approach:** Retain honest Accessibility declarations and foreground disclosure/consent. Prepare for a policy-compliant Google Play release or a classification appeal instead of changing security settings, hiding the service or misrepresenting it.

**Future implication:** Friend-facing installation remains **unresolved** until independent, normal installation succeeds with protection enabled. Preserve the distinction between source code quality, developer ADB testing, Play Protect classification and public distribution.

## 3. LESSONS

1. **Verification hierarchy:** source inspection < successful build < unit/engine tests < real signed-install behavior < physical-user workflow acceptance. Report the achieved level, not a higher one.
2. **State matters in accessibility:** editing focus, keyboard visibility, field identity, package identity, recording/transcribing/pending state, cancellation and cleanup all interact.
3. **Safe overlays are quiet overlays:** position windows only when layout actually changes; avoid event feedback loops.
4. **Keep the signing key and model manifest stable:** install continuity and cryptographic file integrity are part of reliability, not optional details.
5. **Preserve the user's exact current work:** don't reset the working tree to a prior commit simply because documentation or tools were added.
6. **A complete phone test is still needed:** particularly for one-tap ✓ automatic paste, X discard, focus-change safety, hide/restore and touch targets after the offline model is ready.
7. **Play Protect classification is outside an APK build's control:** use a legitimate review/distribution process; never recommend disabling protection as an installation method.
8. **Accessibility compliance is an app-level UX requirement:** prominent, separately consented disclosure and accurate service declarations are necessary for a non-accessibility-tool app.

## 4. MAINTENANCE

- Add problems only if their causes or outcomes were observed; clearly label unconfirmed hypotheses.
- Record failed and successful approaches concisely, with why they failed or succeeded.
- Use numbered decisions and lessons to avoid repeating prior investigations.
- Move detailed feature-progress claims into `Goal and Progress.md`, condensed context into `Memory.md`, and general behavioral instructions into `AGENTS.md`.
- After phone testing arrives, add confirmed symptoms and solutions here **only if** they represent reusable learnings.

---

**Principle:** Problem → evidence → diagnosis → selected remedy → verified outcome → future lesson.
