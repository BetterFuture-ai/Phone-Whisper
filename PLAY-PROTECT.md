# Phone Whisper — Play Protect installation diagnosis and legitimate resolution

**Checked:** 26 October 2026  
**Affected APK:** `Install/PhoneWhisper.apk` — v0.4.4, versionCode 8  
**Package:** `com.jitu.fluidspeech`  
**APK SHA-256:** `18d37b85d414a9468c11e0c17c8f0ebc64418c5626e2458853c76044ad608c30`

## Confirmed root cause

On the OnePlus Nord 4 in India, a fresh, ordinary File Manager installation of **v0.4.3** produced Google Play Protect's hard-block dialog:

> App blocked to protect your device  
> This app can request access to sensitive data. This can increase the risk of identity theft or financial fraud.

It showed **OK** and no normal **Install anyway** action. After v0.4.3 was installed using ADB for developer testing, opening the v0.4.4 APK as an update in File Manager instead showed **App scan recommended**. That is a distinct prompt for unknown apps; it **does not** establish that a fresh consumer install of v0.4.4 would be allowed.

Google's own documentation says the hard-block prompt is part of Enhanced Fraud Protection active in selected markets, **including India**: apps installed from browsers, file managers, or messaging apps that declare one of these high-risk capabilities — SMS reading/receiving, notification listener, or an AccessibilityService — can be blocked automatically **because of the permission and installer source**, regardless of the developer's benign intentions. The warning is not proof that Phone Whisper is malware or that its APK signature is defective.

- Official explanation and warning variants: https://developers.google.com/android/play-protect/warning-dev-guidance
- India rollout: https://blog.google/intl/en-in/products/launching-enhanced-fraud-protection-pilot-in-india/
- Google Play verification/appeals: https://support.google.com/googleplay/android-developer/answer/2992033

### Why this app triggers that protection

The declared `CompanionService` is bound with `android.permission.BIND_ACCESSIBILITY_SERVICE` and handles the `android.accessibilityservice.AccessibilityService` intent. It exists so the user can **keep Gboard selected** while Phone Whisper displays a floating microphone and inserts its offline transcript into an editable field in another app. The service metadata correctly has `android:isAccessibilityTool="false"`: Phone Whisper is a general-purpose voice typing utility, not a tool whose primary purpose is assisting people with disabilities.

The normal APK also declares `RECORD_AUDIO`, `INTERNET`, `FOREGROUND_SERVICE`, and `FOREGROUND_SERVICE_MICROPHONE`. The evidence identifies the Accessibility service as the **relevant sensitive capability**, not SMS or notification monitoring.

## What does NOT fix it

- **Re-signing, renaming, recompiling, or changing the version number** of the same app: doesn't remove the sensitive capability or override Google's installation policy.
- **Offering an in-app disclosure only:** essential for transparency/review, but not a guaranteed override for the *pre-install* high-risk-source blocking rule. v0.4.4 already includes explicit Accessibility-use consent.
- **Using `isAccessibilityTool=true` to avoid the warning:** inaccurate for a general-purpose voice dictation app and may lead to a separate **deceptive accessibility tool** warning.
- **Asking users to turn off Play Protect:** defeats user security; not a safe or acceptable distribution strategy.
- **Installing with ADB:** useful and successful as a developer test, but not a normal friend-facing install route. Avoid telling friends to enable USB debugging to get around Play Protect.
- **Changing installer/source tricks:** don't disguise origin or the Accessibility declaration; not a trustworthy fix.
- **Assuming a scan will unblock:** the **App scan recommended** dialog and the **App blocked to protect your device** dialog are separate Play Protect actions, with different options.

## Practical compliant solutions

### Route A — Google Play internal testing / release (preferred for friends)

Distribute through a Google Play test track after meeting Play's requirements and receiving review/approval. This retains the existing Gboard + floating dictation capability without telling friends to disable protections. It requires the owner's Play Console access, developer verification, and Android policy declarations.

**Build blocker found:** As of 31 August 2026, new Google Play apps and updates must target **Android 16 / API 36**. The current v0.4.4 project targets **API 34**, with the Mac's existing Android SDK platform at **35**. Before a Play submission, install SDK/API 36 tools in an appropriate resource-bounded manner, update target SDK, check Android 16 behavior and permissions carefully, and build an appropriate Play artifact (generally an Android App Bundle). **Do not blindly change the currently installed personal-test APK until the migration is tested.**

Official requirements:
- https://support.google.com/googleplay/android-developer/answer/11926878
- https://support.google.com/googleplay/android-developer/answer/10964491
- https://support.google.com/googleplay/android-developer/answer/9844679

Accessibility-specific Play steps:
- Clearly describe the Gboard companion workflow and limited, user-initiated insertion.
- Keep `isAccessibilityTool=false`; only use `true` if the app's primary real-world purpose genuinely satisfies the disability-assistive definition.
- Show a prominent, separate in-app disclosure with explicit consent. v0.4.4 has one; verify it is shown at the required stage in all entry flows.
- Complete the Play Console AccessibilityService permission declaration and supply the required demonstration/details when asked.
- Supply a truthful privacy policy and disclosures for microphone capture, local transcript handling, model downloads, optional encrypted download token, and network access.

**Important:** Google Play review and acceptance are **not automatic**, and this preparation does not promise approval.

### Route B — Play Protect classification appeal

If the developer believes the high-risk-source block is misapplied to this transparent utility, Google offers a classification appeal:

https://support.google.com/googleplay/android-developer/contact/protectappeals

As of this check, the form asks for:
- Contact email (owner must provide).
- Developer name (owner must provide).
- Package: `com.jitu.fluidspeech`.
- SHA-256 of the APK that **has been uploaded to VirusTotal**. The local checksum for v0.4.4 is above, but **a local checksum alone does not satisfy the uploaded-to-VirusTotal condition**.
- Additional explanatory information.

**Privacy precaution:** VirusTotal can share uploaded files with security researchers and partners. Uploading this personal APK to VirusTotal or submitting the form should happen **only after explicit owner approval**. No upload or appeal submission has been performed. The form warns that decisions are final and it may not respond.

#### Proposed appeal description (owner review required)

> Phone Whisper (package com.jitu.fluidspeech) is an offline, user-initiated English voice dictation utility for Android. Users keep their existing Gboard keyboard, and can explicitly start and stop dictation using a floating control. The app uses Android AccessibilityService only for detecting an active editable field and keyboard and committing the user's own dictated text into that field, with field-identity safety checks and password-field exclusions. Its microphone captures speech only during a user-initiated session; transcription runs locally using an ONNX model. The app does not use SMS-reading or notification-listener permissions, does not capture credentials, and does not upload recordings or transcripts to a cloud recognition service. The Accessibility capability is accurately disclosed in-app with a separate affirmative consent dialog, and the service is not represented as an accessibility tool for disabled users. The app has been blocked during normal installation from OnePlus File Manager on a OnePlus Nord 4 in India with the Play Protect sensitive-permission fraud warning. We request a review of the classification and guidance, where available, for distribution without asking users to weaken Play Protect.

Do not describe this as an already-approved accessibility tool or a guaranteed benign classification. Verify all claims against the final build before submission.

### Route C — redesign without AccessibilityService (changes the product)

A version **without any AccessibilityService** could avoid this *specific sensitive-permission* trigger, but the app could not keep the exact same seamless Gboard + arbitrary-text-field automatic insertion architecture. Options include copying text to clipboard for manual paste or requiring the user to select Phone Whisper as their input method (replacing Gboard while active). Those would alter a core requirement and should **not** be silently substituted.

## Evidence / current state

- v0.4.4 build and ONNX smoke tests passed; APK v3 signature verified.
- Physical phone reported v0.4.4 installed through ADB. The hard block was observed for a **fresh v0.4.3 File Manager installation**, and the update prompt for v0.4.4 was **App scan recommended**.
- No Play Protect setting was disabled, no VirusTotal upload was made, no appeal sent, and no Google Play release was published.
- An install succeeding by ADB **does not** mean a first-time phone owner can install by tapping the same APK.
- Don't uninstall the current Android app solely to reproduce the warning: that could delete its private model/settings and is not necessary given the direct evidence and published policy.

**Resolution status:** Root cause **confirmed**. A friend-friendly installation route retaining the Accessibility-based Gboard companion is **not yet delivered**; completing Route A or obtaining a favorable Google review under Route B is the responsible next step.
