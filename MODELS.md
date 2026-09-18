# SafePay Shield AI Models

SafePay Shield uses on-device inference to enhance its risk analysis. All inference happens locally; no data ever leaves the phone.

## Model Contracts

### 1. Visual Tamper Detection (`visual_tamper.tflite`)
- **Input**: `[1, 224, 224, 3]` (RGB Float normalized 0-1).
- **Task**: Identify image manipulation signs in QR captures.
- **Output**: `[1, 1]` (Probability 0.0 - 1.0).
- **Fallback**: Heuristic-based analysis of image features (contrast, alignment).

### 2. Payload Risk Analysis (`payload_risk.tflite`)
- **Input**: `[1, 128]` (Tokenized UPI note/address).
- **Task**: Detect suspicious patterns in payment metadata.
- **Output**: `[1, 1]` (Risk probability).
- **Fallback**: Regex-based keyword matching and structural validation.

## Performance Optimization
- Models are loaded into memory on-demand.
- Inference is kept off the main UI thread.
- Deterministic CPU fallback ensures 100% availability even without model assets.

---
*SafePay Shield AI - Privacy through On-Device Learning.*
