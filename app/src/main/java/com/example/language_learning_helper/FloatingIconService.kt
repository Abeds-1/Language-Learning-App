package com.example.language_learning_helper

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Handler
import android.os.Looper
import android.content.BroadcastReceiver
import android.content.IntentFilter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.os.Environment

class FloatingIconService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: FrameLayout
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var wordView: WordView

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var isCapturing = false

    private val recognitionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.language_learning_helper.RENDER_COMPLETE") {
                println("Render complete.")
                println("hello from render complete")
                // // After receiving broadcast, send another broadcast to finish ScanningEffectActivity
                // val finishIntent = Intent("com.example.language_learning_helper.RECOGNITION_COMPLETE")
                // sendBroadcast(finishIntent)
                
                // Step 2: Initialize MediaProjection and capture the screenshot
                initializeMediaProjection()
                // Notify the activity to pause the scanning effect
                val broadcastIntent = Intent("com.example.language_learning_helper.PAUSE_SCANNING").apply {
                }
                sendBroadcast(broadcastIntent)
                }

            if(intent.action == "com.example.language_learning_helper.TAKE_SCREENSHOT"){
                
                val selectionIntent = Intent(this@FloatingIconService, SelectionActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(selectionIntent)
            
                captureScreenshot()
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "floating_icon_service_channel"
        private const val NOTIFICATION_ID = 1
        var mediaProjectionData: Intent? = null
        var resultCode: Int = 0
        var isServiceRunning = false

        fun startService(context: Context) {
            val intent = Intent(context, FloatingIconService::class.java)
            context.startService(intent)
            isServiceRunning = true
        }   
    }

    override fun onCreate() {
        super.onCreate()
    
        // Create the notification channel and start the service in the foreground
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    
        // Set up the floating icon
        setupFloatingIcon()
    
        // Register the BroadcastReceiver with an IntentFilter
        val filter = IntentFilter().apply {
            addAction("com.example.language_learning_helper.RENDER_COMPLETE")
            addAction("com.example.language_learning_helper.TAKE_SCREENSHOT") // Add all relevant actions
        }
    
        try {
            registerReceiver(recognitionReceiver, filter)
            println("BroadcastReceiver registered successfully in FloatingIconService")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to register BroadcastReceiver: ${e.message}")
        }
    }
    

    private fun setupFloatingIcon() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_icon, null) as FrameLayout
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false
            private val touchSlop = 10

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (!isDragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                            isDragging = true
                        }
                        if (isDragging) {
                            params.x = initialX + dx
                            params.y = initialY + dy
                            windowManager.updateViewLayout(floatingView, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            if (!isCapturing){ 
                                println("Icon clicked")
                                
                                val scanningIntent = Intent(this@FloatingIconService, ScanningEffectActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                }
                                startActivity(scanningIntent)
                                }
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(floatingView, params)

        // Add WordView overlay
        wordView = WordView(this, null)
    }

    private fun initializeMediaProjection() {
        println("initializing media projection")
        if (mediaProjectionData == null) {
            Toast.makeText(this, "MediaProjection permission not granted.", Toast.LENGTH_SHORT).show()
            return
        }
        val mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, mediaProjectionData!!)
    }

    

    private fun captureScreenshot() {
        if (isCapturing) {
            Toast.makeText(this, "Screenshot already in progress.", Toast.LENGTH_SHORT).show()
            return
        }
    
        try {
            isCapturing = true
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            // ImageReader to capture the screen
            val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1)
            val surface = imageReader.surface
    
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenshotCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
            )
    
            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    image.use {
                        val planes = it.planes
                        val buffer = planes[0].buffer
                        val fullBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        fullBitmap.copyPixelsFromBuffer(buffer)
    
                        // // Calculate StatusBar and NavBar heights explicitly
                        // val statusBarHeight = resources.getIdentifier("status_bar_height", "dimen", "android").let { resId ->
                        //     if (resId > 0) resources.getDimensionPixelSize(resId) else 0
                        // }
    
                        // val navBarHeight = resources.getIdentifier("navigation_bar_height", "dimen", "android").let { resId ->
                        //     if (resId > 0) resources.getDimensionPixelSize(resId) else 0
                        // }
    
                        // println("StatusBarHeight=$statusBarHeight, NavBarHeight=$navBarHeight, Width=$width, Height=$height")
    
                        // // Crop the bitmap to exclude bars
                        // val croppedBitmap = Bitmap.createBitmap(
                        //     fullBitmap,
                        //     0, // X offset
                        //     statusBarHeight,  // Y offset
                        //     width,  // Full width
                        //     height - statusBarHeight - navBarHeight // Visible height
                        // )
    
                        
                        // Save the cropped image to the Pictures directory
                        //saveImageToPublicDirectory(croppedBitmap)
    
                        // Notify the activity to resume the scanning effect
                        SelectionActivity.bitmap = fullBitmap
                        val broadcastIntent = Intent("com.example.language_learning_helper.RESUME_SCANNING").apply {}
                        sendBroadcast(broadcastIntent)
                    }
                    reader.close()
                    stopScreenCapture()
                    isCapturing = false
                }
            }, null)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(applicationContext, "Error capturing screenshot: ${e.message}", Toast.LENGTH_LONG).show()
            isCapturing = false // Reset in case of failure
        }
    }
    
    
    
    // Save the cropped image to the Pictures directory
fun saveImageToPublicDirectory(croppedBitmap: Bitmap) {
    // Get the Pictures directory (public external storage directory)
    val picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

    // Create a new file in the Pictures directory
    val file = File(picturesDirectory, "cropped_image_${System.currentTimeMillis()}.jpg")

    // Create a file output stream to write the bitmap data to the file
    try {
        val outputStream = FileOutputStream(file)
        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream) // Compress bitmap and write to file
        outputStream.flush()
        outputStream.close()


    } catch (e: IOException) {
        e.printStackTrace()
        println("Error saving image: ${e.message}")
    }
}
    
    

                   
    private fun handleScreenshotCapture() {
        println("Handling screenshot")
        
       
        
    }
    
    
    

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(floatingView)
        windowManager.removeView(wordView)
        isServiceRunning = false
        stopScreenCapture()
        unregisterReceiver(recognitionReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Icon Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("Floating Icon Service")
            .setContentText("Tap the floating icon to capture the screen.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun stopScreenCapture() {
        virtualDisplay?.release()
        mediaProjection?.stop()
        virtualDisplay = null
        mediaProjection = null
    }
}
