# SafePay Shield (NFC & Offline Enhanced)

SafePay Shield is a privacy-first Android security layer for UPI payments. It protects users by analyzing payment requests from QR codes, NFC tags, and deep links locally on-device using a multi-layer risk engine.

## New Features
- **NFC Tap & Pay Security**: Scan NFC payment tags to verify payee legitimacy before proceeding.
- **Offline Security Mode**: Perform 100% local risk analysis without needing a data connection.
- **Offline Demo Mode**: A dedicated flow to demonstrate security validation (Biometrics + Risk Analysis) even without real payment infrastructure.
- **Unified Payment Model**: All payment sources (QR, NFC, URI) are normalized into a common security pipeline.

### Core Architecture
- **NFC Manager**: Handles Android NFC `ReaderMode` and NDEF parsing for UPI URIs.
- **Risk Engine**: 4-layer weighted analysis:
  1. **Visual Tamper** (QR only)
  2. **Payload Analysis** (Keyword matching + regex)
  3. **Merchant Verification** (Local trust registry)
  4. **Behavioral Anomaly** (Local history patterns)
- **Biometric Layer**: AndroidX `BiometricPrompt` for high-risk (CAUTION) and Offline Demo authorizations.
- **Secure Persistence**: Room DB for audit history and EncryptedSharedPreferences for configuration.

### Security Implementation
- **NFC Replay Protection**: Fingerprinting recent payloads to detect rapid repeated taps.
- **Payload Validation**: Strict normalization of VPA, Amount, and Merchant info.
- **Hardening**: `FLAG_SECURE`, obscured-touch protection, and disabled backups.

## Demo Scenarios
1. **QR SAFE**: Scan a verified merchant QR -> Direct handoff to payment.
2. **NFC CAUTION**: Tap a suspicious NFC tag -> Biometric prompt required.
3. **NFC BLOCK**: Tap a known fraud tag -> Payment blocked with explanation.
4. **Offline Demo**: "Run Offline Demo" from Tap & Pay screen to see the full security flow.

---
*Note: SafePay Shield is a security prototype. It does not independently settle bank transactions.*
