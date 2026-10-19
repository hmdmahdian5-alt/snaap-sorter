package com.delivery.assistant

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.Locale
import java.util.concurrent.Executors

class FloatingScannerService : LifecycleService(), TextToSpeech.OnInitListener {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: LinearLayout
    private lateinit var resultTextView: TextView
    private lateinit var tts: TextToSpeech
    private var lastScannedCode = ""

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        startForegroundNotification()
        createFloatingView()
    }

    private fun startForegroundNotification() {
        val channelId = "floating_scanner_ch"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "اسکنر مرسولات", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("اسکنر مرسولات شاپ فعال است")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
        startForeground(101, notification)
    }

    private fun createFloatingView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            450, 550,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        overlayView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(16, 16, 16, 16)
        }

        resultTextView = TextView(this).apply {
            text = "آماده اسکن"
            setTextColor(Color.YELLOW)
            textSize = 22f
            gravity = Gravity.CENTER
        }

        val previewView = PreviewView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 350
            )
        }

        overlayView.addView(resultTextView)
        overlayView.addView(previewView)
        windowManager.addView(overlayView, params)

        startCamera(previewView)
    }

    private fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also {
                    it.setAnalyzer(Executors.newSingleThreadExecutor(), ::processImageProxy)
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { handleScannedBarcode(it) }
                    }
                }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    private fun handleScannedBarcode(raw: String) {
        if (raw == lastScannedCode) return
        lastScannedCode = raw

        // استخراج ارقام سفارش (مانند SHP1585296745-1 -> 1585296745)
        val match = Regex("\\d{6,12}").find(raw)
        val orderId = match?.value

        val stopNumber = StopRepository.destinationMap[orderId]
        val message = if (stopNumber != null) "مقصد $stopNumber" else "شناسه نامعتبر!"

        resultTextView.post {
            resultTextView.text = message
        }

        // اعلام صوتی شماره مقصد
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "TTS_STOP")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("fa")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
        if (::overlayView.isInitialized) windowManager.removeView(overlayView)
    }
}
