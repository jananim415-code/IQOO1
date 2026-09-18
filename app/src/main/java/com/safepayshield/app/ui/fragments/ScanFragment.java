package com.safepayshield.app.ui.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.safepayshield.app.data.models.NormalizedPaymentRequest;
import com.safepayshield.app.databinding.FragmentScanBinding;
import com.safepayshield.app.ui.ElderModeHelper;
import com.safepayshield.app.ui.main.MainViewModel;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ScanFragment extends Fragment {

    private static final int PERMISSION_CODE = 101;
    private FragmentScanBinding binding;
    private MainViewModel viewModel;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentScanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ElderModeHelper.apply(requireContext(), view);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        cameraExecutor = Executors.newSingleThreadExecutor();

        checkPermissionAndStart();

        binding.btnCancel.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
    }

    private void checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(requireContext())
                .setTitle("Camera Permission")
                .setMessage("Camera access is needed to scan payment QR codes.")
                .setPositiveButton("Grant", (d, w) -> requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_CODE))
                .setNegativeButton("Deny", (d, w) -> showPermissionDenied())
                .show();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_CODE);
        }
    }

    private void showPermissionDenied() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Permission Denied")
            .setMessage("Without camera access, you cannot scan QR codes. Please enable it in Settings.")
            .setPositiveButton("Settings", (d, w) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
                startActivity(intent);
            })
            .setNegativeButton("Cancel", (d, w) -> Navigation.findNavController(requireView()).popBackStack())
            .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                showPermissionDenied();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                viewModel.showError("Failed to start camera");
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @SuppressWarnings("UnsafeOptInUsageError")
    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::processImageProxyWrapper);

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);
    }

    @SuppressWarnings("UnsafeOptInUsageError")
    private void processImageProxyWrapper(ImageProxy image) {
        processImageProxy(image);
    }

    @ExperimentalGetImage
    private void processImageProxy(ImageProxy image) {
        BarcodeScanner scanner = BarcodeScanning.getClient();
        Image mediaImage = image.getImage();
        if (mediaImage != null) {
            InputImage inputImage = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
            scanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        if (!barcodes.isEmpty()) {
                            String rawValue = barcodes.get(0).getRawValue();
                            if (rawValue != null && rawValue.startsWith("upi://")) {
                                NormalizedPaymentRequest request = new NormalizedPaymentRequest(
                                    NormalizedPaymentRequest.Source.QR, null, null, null, null, rawValue, false);
                                requireActivity().runOnUiThread(() -> {
                                    cameraProvider.unbindAll(); // Stop scanning
                                    viewModel.analyzePaymentRequest(request);
                                });
                            }
                        }
                    })
                    .addOnCompleteListener(task -> image.close());
        } else {
            image.close();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
    }
}
