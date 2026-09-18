package com.safepayshield.app.ai;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import com.safepayshield.app.data.models.VisualFeatures;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public final class OnDeviceInference {
    private static final String TAG = "SafePayShieldAI";
    private Interpreter visualInterpreter;
    private Interpreter payloadInterpreter;

    public OnDeviceInference(Context context) {
        visualInterpreter = loadModel(context, "visual_tamper.tflite");
        payloadInterpreter = loadModel(context, "payload_risk.tflite");
        
        if (visualInterpreter == null) Log.i(TAG, "Visual model not found, using rules");
        if (payloadInterpreter == null) Log.i(TAG, "Payload model not found, using rules");
    }

    public double getVisualScore(VisualFeatures features) {
        // Mocking for prototype: If model exists, it would run here. 
        // User requested: deterministic CPU fallback when models are unavailable.
        return ((1 - features.contrast) + (1 - features.edgeContinuity) + features.occlusion + (1 - features.alignment)) / 4.0;
    }

    public double getPayloadScore(String note, String vpa) {
        if (note != null && note.toLowerCase().matches(".*(urgent|refund|otp|kyc|prize).*")) {
            return 0.82;
        }
        if (vpa != null && vpa.length() > 45) {
            return 0.62;
        }
        return 0.12;
    }

    private Interpreter loadModel(Context context, String assetName) {
        try {
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd(assetName);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            return new Interpreter(buffer);
        } catch (Exception e) {
            return null;
        }
    }
    
    public void close() {
        if (visualInterpreter != null) visualInterpreter.close();
        if (payloadInterpreter != null) payloadInterpreter.close();
    }
}
