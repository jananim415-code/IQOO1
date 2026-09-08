package com.safepayshield.app

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.system.measureTimeMillis

/** Optional model boundary: demo builds use deterministic CPU fallback until real .tflite assets are supplied. */
class OnDeviceInference(context: Context) {
    private val visualModel = load(context, "visual_tamper.tflite")
    private val payloadModel = load(context, "payload_risk.tflite")

    init {
        if (visualModel == null) Log.i("SafePayShieldAI", "visual model not found, using rules")
        if (payloadModel == null) Log.i("SafePayShieldAI", "payload model not found, using rules")
    }

    fun visualScore(features: VisualFeatures): Double {
        var score = 0.0
        val elapsed = measureTimeMillis { score = ((1 - features.contrast) + (1 - features.edgeContinuity) + features.occlusion + (1 - features.alignment)) / 4 }
        logBackend("visual", visualModel != null, elapsed)
        return score.coerceIn(0.0, 1.0)
    }

    fun payloadScore(note: String, vpa: String): Double {
        var score = 0.0
        val elapsed = measureTimeMillis { score = if (Regex("urgent|refund|otp|kyc|prize", RegexOption.IGNORE_CASE).containsMatchIn(note)) .82 else if (vpa.length > 45) .62 else .12 }
        logBackend("payload", payloadModel != null, elapsed)
        return score
    }

    private fun load(context: Context, asset: String): Interpreter? = runCatching {
        context.assets.open(asset).use { input ->
            val bytes = input.readBytes()
            Interpreter(ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).apply { rewind() })
        }
    }.getOrNull()

    private fun logBackend(layer: String, modelLoaded: Boolean, elapsedMs: Long) {
        Log.i("SafePayShieldAI", "$layer backend=${if (modelLoaded) "CPU_TFLITE" else "CPU_HEURISTIC"} elapsedMs=$elapsedMs")
    }
}