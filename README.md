# SafePay Shield

SafePay Shield is an Android-only, phone-first UPI safety layer. It checks a QR/deep link on-device before a payment handoff and explains a transparent SAFE, CAUTION, or BLOCK decision.

## Build and run

Open the project in Android Studio Hedgehog or newer, sync Gradle, and run the `app` configuration on an Android 8+ phone. Grant camera permission for the CameraX scan surface. The project targets SDK 35 and uses Java/Kotlin 17.

The current workspace does not include a Gradle wrapper and the preparation environment has no Android SDK, Gradle, or ADB, so APK compilation and physical-device installation must be performed from Android Studio on a machine with those tools installed.

## Architecture

```text
CameraX / upi:// intent
	|
Input normalization + ML Kit QR decode
	|
Visual tamper -> Payload risk -> Merchant/VPA verification -> Behavior anomaly
	\________________ weighted composite (25 / 35 / 25 / 15) ________________/
			      SAFE / CAUTION / BLOCK
```

`RiskEngine` exposes normalized 0..1 layer calculations, local verified and fraud VPA registries, keyword/look-alike/amount checks, late-night behavior checks, bounded reasons, and weighted decision thresholds. `OnDeviceInference` is the model boundary: it loads optional `visual_tamper.tflite` and `payload_risk.tflite` assets and records backend/timing without payment data. When assets are absent, the deterministic CPU heuristic keeps the offline demo usable. A production build can add LiteRT/QNN delegate selection in this class.

`SecureStore` uses AES-GCM with an Android Keystore key for settings, contacts, merchants, history, and local actions. No analytics or crash-reporting SDK is present. The activity enables `FLAG_SECURE`, filters obscured touches, disables cleartext traffic/backups, checks screen-lock state, and warns about non-TalkBack accessibility services.

## Two-minute demo

1. Launch and tap **Normal merchant QR**. It produces SAFE for `verified@okaxis`; show the layer bars, then use **Proceed to pay** or return home.
2. Tap **Urgent refund QR**. It produces CAUTION from urgency language and an untrusted VPA. Use **Verify with family**, then show the local request confirmation or **Pause payment**.
3. Tap **Fraud + tampered QR**. It produces BLOCK from the tamper marker, fraud VPA, OTP language, and high amount. Use **Block & Report**; the report is saved locally.
4. Open **Settings**. Toggle English/Tamil, Elder Mode, and Family approval. Add/remove a contact or merchant. Open **Privacy & how it works** and test **Clear all local data**.
5. Use **Scan QR** for a real UPI QR in good lighting, or paste an `upi://pay?...` link. A link opened by another app is accepted through the `upi` intent filter and analyzed before the optional handoff.

## Sample links

```text
SAFE:    upi://pay?pa=verified@okaxis&pn=City%20Cafe&am=249&tn=Lunch
CAUTION: upi://pay?pa=unknown-shop@upi&pn=Quick%20Refund&am=2400&tn=Urgent%20refund%20claim%20now
BLOCK:   upi://pay?pa=refunddesk@ybl&pn=Refund%20Desk&am=75000&tn=OTP%20required%20immediately%20tampered
```

When Family approval is enabled, the CAUTION proceed button becomes **Request family approval**. Approval is stored encrypted and is only accepted when a trusted contact exists. BLOCK never exposes a proceed action. Clearing local data resets the in-memory lists immediately and shows a snackbar confirmation.

Model input/output contracts are documented in [MODELS.md](MODELS.md). Placeholder assets are present for packaging tests; invalid or missing binaries automatically use the rules fallback.

## Privacy and production gaps

Core prototype data is encrypted and local-only. The current repository is an encrypted preferences-backed prototype rather than Room/SQLCipher; replace `SecureStore` with an encrypted Room/SQLCipher repository before storing a larger audit database. Binary AI assets are intentionally not bundled; add validated, quantized models under `app/src/main/assets` and perform formal model, security, and NPCI review before release. The UPI handoff is a test intent and must be integrated with an approved PSP flow for production.

## Device test checklist

- Scan a QR in bright, dim, and angled lighting.
- Open each sample `upi://` link from a browser or messaging app.
- Toggle Elder Mode and language, then verify back navigation.
- Verify history replay, contact/merchant removal, clear-data behavior, screen-lock warning, and accessibility warning.
- Confirm no VPA, amount, payee, or QR image is written to logcat.
