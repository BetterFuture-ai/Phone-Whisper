# Install Phone Whisper on Android

[← Back to the project](README.md) · [Get the latest APK](https://github.com/BetterFuture-ai/Phone-Whisper/releases/latest)

**Phone Whisper v0.4.4** · Android 13+ · ARM64 · Offline English speech-to-text

This is a user-built application, **not currently available from Google Play**. It requires permission to use the microphone and—only for the floating microphone alongside Gboard—Android **Accessibility** access. Read the warnings before granting access.

## 1. Download the app

Visit this project's [GitHub Releases](https://github.com/BetterFuture-ai/Phone-Whisper/releases), open the latest release, expand **Assets**, and choose **`PhoneWhisper.apk`**. GitHub's **Source code (zip)** option is *not* an installable APK.

Verify the file against the published release's SHA-256 checksum:

```text
Version: v0.4.4
Package: com.jitu.fluidspeech
APK SHA-256: 18d37b85d414a9468c11e0c17c8f0ebc64418c5626e2458853c76044ad608c30
```

On a computer with SHA-256 tooling, run `shasum -a 256 PhoneWhisper.apk` and compare the output. The file name is not proof of authenticity; only the exact matching checksum verifies that it is the published binary.

## 2. Play Protect and app installation

> [!CAUTION]
> **Google Play Protect may block installation, especially in India.** This is because Phone Whisper's Gboard companion is declared as an **AccessibilityService**, one of the permissions covered by Google's Enhanced Fraud Protection for apps installed through a browser or file manager. The observed warning is **“App blocked to protect your device”**. This hard block does **not** always offer an **“Install anyway”** button.
>
> The fact that an application is open source or APK-signed does not mean Android will trust it automatically. There is also no guarantee that the separate **“Scan app”** prompt will override a sensitive-permission hard block.

**Recommended:** Keep Play Protect enabled. Do not install if you are uncomfortable with Accessibility access or can't verify where the file came from.

Some users elect to temporarily suspend Play Protect scanning to install software they personally reviewed. **That reduces protection against malicious apps and isn't recommended as a general installation procedure.** If you choose that risk on your own device, Android's controls are usually at **Play Store → profile picture → Play Protect → settings gear → Scan apps with Play Protect**. This differs across Android versions and may not override every restriction. **Re-enable scanning as soon as you finish and confirm Play Protect is active again.** Do not change these settings on another person's device without their informed agreement.

For technical developers, Android Debug Bridge (ADB) can install an APK with a USB-debugging connection:

```sh
adb install -r PhoneWhisper.apk
```

ADB installation is meant for development and verification, **not a general solution for sharing APKs with friends**. You do not need to enable USB debugging for normal app usage.

Further background and official Google policy links: [PLAY-PROTECT.md](PLAY-PROTECT.md).

## 3. Set up the offline speech model

1. Launch **Phone Whisper**.
2. Select the **Model** tab (or tap **Finish setup** from Home).
3. Press **Download model · 300 MB** and keep the screen open. This is normally a one-time download.
4. If the provider is temporarily unavailable, use the small **gear beside Download model** to open the source popup. You may configure an HTTPS mirror with exactly matching model files or supply an optional Hugging Face read-only token. The downloaded model is hash-verified.
5. When ready, the app will indicate the model is available. Model loading may take a little time on first use.

Do not uninstall an already working app unless necessary: uninstalling deletes its private model data and preferences. Updating with a correctly signed newer version normally retains them.

## 4. Allow the microphone

On the Home screen, tap **Allow your microphone** and grant the Android microphone permission. Phone Whisper records **only while you start a session**; a microphone indicator/notification may appear during recording. Transcription itself runs offline.

## 5. Enable the floating voice button alongside Gboard

Keep **Gboard** selected as your existing keyboard.

1. In Phone Whisper **Home**, tap **Keep your current keyboard** or open **Settings → Enable companion voice button**.
2. Read Phone Whisper's **Accessibility access for voice typing** explanation. It describes how the service can detect the active editable field and insert your dictated text. Continue only if you agree.
3. In Android Settings, find **Accessibility → Downloaded apps** or **Installed services**. On some OnePlus/ColorOS versions, this may be under **Accessibility & convenience**.
4. Tap **Phone Whisper voice button**. It may be disabled or appear inside the **Downloaded apps** section initially.
5. If your device displays **View more**, **More details**, or a security explanation, open it and read it carefully. This is Android's own permission notice; the exact wording differs by phone.
6. If Android says **Restricted setting** or prevents enabling the service, that is an additional Android sideloading safety control. You can inspect the device's **Settings → Apps → Phone Whisper → App info** and its security/settings information. Only authorize Accessibility access after understanding its potential to observe on-screen information.
7. If you approve, turn **Use Phone Whisper voice button** on and accept Android's explicit Accessibility permission confirmation. Return to Phone Whisper.

**Why does it need this powerful permission?** To detect a keyboard/editor window and insert voice-generated text **without replacing Gboard**. It is not intended to read passwords, and the app excludes recognized password fields. Nevertheless, Accessibility is sensitive access: **only enable services you trust**.

## 6. Talk, finish, or cancel

1. Open any ordinary editable field (messages, notes, a draft, etc.) and show **Gboard**.
2. The floating Phone Whisper bubble should appear. Tap it to begin dictation.
3. A **✕** is on the left; a **✓** is on the right.
4. Press **✓ once** to finish. The model transcribes locally and pastes into the same field when done—**no second tap or review popup**.
5. Press **✕** to discard without inserting. The app checks that the original text field has not changed before pasting.

**Move it:** Touch and drag the voice bubble. Drag to the red dismiss area above the Android navigation zone to hide the control. Open Phone Whisper **Home → Show floating button** to restore it. Adjust button size, opacity, animations, and action styles from Settings.

## 7. Personalize

- **Model:** offline model status, preload/unload, performance threads, recording length and source settings.
- **Words:** correct frequently misrecognized names and create spoken snippets; export/import backups.
- **History:** optionally save up to 20 dictations on the phone; history is off by default.
- **Settings:** action button size/style, floating mic image, wave animation, idle behavior and permissions.

## Troubleshooting

| Problem | What to check |
|---|---|
| APK blocked by Play Protect | Read section 2. No guaranteed “Install anyway” option exists for certain sideloaded Accessibility apps. |
| Accessibility switch is unavailable | Review Android's restricted-setting notice. This is a platform protection, not necessarily a software crash. |
| Bubble does not appear | Enable **Phone Whisper voice button** under Accessibility, ensure **Show the companion voice button** is enabled in app Settings, and open an editable field with Gboard. If previously hidden, restore from Home. |
| Nothing transcribes | The model must show ready, microphone permission must be allowed, and recording must finish. Check storage space and model integrity. |
| Wrong text field | Switches during transcription intentionally discard results rather than paste into a different field. |
| Download failed | Check network access, available storage, or use the model-source configuration gear. Model files are pinned by SHA-256. |
| Another keyboard appears | Keep Gboard set as Android's current default keyboard. The optional Phone Whisper keyboard is not required. |
| APK update fails | A source build uses a different signing key than the published release. Update only with the same signed app lineage, or uninstall and accept loss of local app data. |

For a technical look at the installation warning, including sources and a Google classification review route, see [PLAY-PROTECT.md](PLAY-PROTECT.md). For sensitive-permission details, see [SECURITY.md](SECURITY.md).

---

**One download. Your keyboard. Your words.**
