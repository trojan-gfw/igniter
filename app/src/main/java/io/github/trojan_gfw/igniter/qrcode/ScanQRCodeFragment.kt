package io.github.trojan_gfw.igniter.qrcode

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import io.github.trojan_gfw.igniter.LogHelper
import io.github.trojan_gfw.igniter.R
import io.github.trojan_gfw.igniter.common.app.BaseFragment
import kotlinx.android.synthetic.main.fragment_scan_qr_code.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanQRCodeFragment : BaseFragment(), ImageAnalysis.Analyzer {
    private lateinit var cameraExecutor: ExecutorService
    private val mainThreadExecutor: Executor by lazy { ContextCompat.getMainExecutor(mContext) }
    private val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder()
            .apply { setBarcodeFormats(Barcode.FORMAT_QR_CODE) }.build())
    private val imageAnalysis: ImageAnalysis by lazy {
        ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(Size(DEFAULT_IMAGE_ANALYSIS_WIDTH, DEFAULT_IMAGE_ANALYSIS_HEIGHT))
                .build().also { it.setAnalyzer(cameraExecutor, this@ScanQRCodeFragment) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_scan_qr_code, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(mContext)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this@ScanQRCodeFragment,
                        CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
            } catch (exception: Exception) {
                LogHelper.e(TAG, "Bind camera failed: " + exception.message)
                Toast.makeText(mContext.applicationContext, R.string.scan_qr_code_camera_error, Toast.LENGTH_SHORT).show()
                finishActivity()
            }
        }, mainThreadExecutor)
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val img = imageProxy.image ?: return
        scanner.process(InputImage.fromMediaImage(img, imageProxy.imageInfo.rotationDegrees))
                .addOnSuccessListener { barcodes ->
                    barcodes.forEach { LogHelper.i(TAG, "scanned ${it.rawValue}") }
                    val qrCodes = barcodes.mapNotNull { it.rawValue ?: "" }
                    val trojanUrl = qrCodes.firstOrNull { it.startsWith("trojan://") }
                    LogHelper.i(TAG, "onSuccess $trojanUrl")
                    trojanUrl?.apply {
                        imageAnalysis.clearAnalyzer()
                        activity?.apply {
                            setResult(Activity.RESULT_OK, Intent().putExtra(KEY_SCAN_CONTENT, trojanUrl))
                            finish()
                        }
                    }
                }.addOnCanceledListener {
                    LogHelper.i(TAG, "onCancelled")
                }.addOnFailureListener { exception ->
                    imageAnalysis.clearAnalyzer()
                    LogHelper.i(TAG, "onFailed ${exception.message}")
                    showScanQRCodeError()
                    imageProxy.close()
                }.addOnCompleteListener {
                    LogHelper.i(TAG, "onComplete")
                    imageProxy.close()
                }
    }

    private fun showScanQRCodeError() {
        val context = mContext.applicationContext
        runOnUiThread {
            Toast.makeText(context, R.string.scan_qr_code_camera_error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        const val TAG = "ScanQRCodeFragment"
        private const val KEY_SCAN_CONTENT = ScanQRCodeActivity.KEY_SCAN_CONTENT
        private const val DEFAULT_IMAGE_ANALYSIS_WIDTH = 1080
        private const val DEFAULT_IMAGE_ANALYSIS_HEIGHT = 1920

        @JvmStatic
        fun newInstance(): ScanQRCodeFragment {
            return ScanQRCodeFragment()
        }
    }
}