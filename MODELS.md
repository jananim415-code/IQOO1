# SafePay Shield model contracts

The app loads optional TensorFlow Lite/LiteRT assets from `app/src/main/assets`. Missing or invalid assets are caught and use the local rules fallback.

## `visual_tamper.tflite`

- Input: one RGB float tensor shaped `[1, 224, 224, 3]`, normalized to `[0, 1]`.
- Output: one float tensor shaped `[1, 1]`.
- Meaning: tamper probability in `[0, 1]`, where `1` means strong evidence of overlay, occlusion, alignment, or print inconsistency.
- Production: quantized INT8 MobileNetV3-small or equivalent, with representative QR-image calibration data.

## `payload_risk.tflite`

- Input: one tokenized structured-payload tensor shaped `[1, 128]`, with fixed vocabulary IDs for `pa`, `pn`, `am`, `tn`, and scheme markers.
- Output: one float tensor shaped `[1, 1]`.
- Meaning: suspicious-payload probability in `[0, 1]`.
- Production: small MobileBERT/DistilBERT-style classifier exported for LiteRT, with no raw payload logging.

`OnDeviceInference` currently exposes the load/backend/timing boundary but the demo assets are placeholders. Qualcomm QNN delegate selection should be added there once the target Snapdragon SDK and validated delegate artifact are available.