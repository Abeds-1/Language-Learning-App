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
import android.view.GestureDetector
import android.widget.FrameLayout
import android.widget.RelativeLayout
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
import android.graphics.Point
import android.view.animation.OvershootInterpolator
import android.animation.ObjectAnimator
import android.content.res.Resources
import android.widget.ImageView
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.Gravity
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.CountDownTimer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import com.caverock.androidsvg.SVG
import android.graphics.drawable.PictureDrawable
import android.app.PendingIntent
import android.app.ActivityManager


class FloatingIconService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var paramRemove: WindowManager.LayoutParams
    private lateinit var servicePreferencesHelper: ServiceSharedPreferencesHelper
    private lateinit var chatHeadView: RelativeLayout
    private lateinit var removeView: RelativeLayout 

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
 
    private var chatHeadImg: ImageView? = null
    private var removeImg: ImageView? = null
    private var xInitCord = 0
    private var yInitCord = 0
    private var xInitMargin = 0
    private var yInitMargin = 0
    private val szWindow = Point()
    private var isLeft = true
    private var startX = 0f
    private var startY = 0f
    private var endX = 1f
    private var endY = 1f
    
    private var isScanningModeOn = true

    private val recognitionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.language_learning_helper.RENDER_COMPLETE") {
                println("Render complete.")
                println("hello from render complete")
                // // After receiving broadcast, send another broadcast to finish ScanningEffectActivity
                // val finishIntent = Intent("com.example.language_learning_helper.RECOGNITION_COMPLETE")
                // sendBroadcast(finishIntent)
                
                startX = intent.getFloatExtra("startX", 0f)
                startY = intent.getFloatExtra("startY", 0f)
                endX = intent.getFloatExtra("endX", 1f)
                endY = intent.getFloatExtra("endY", 1f)
                println("from floating render complete\n startX${startX} startY${startY} endX${endX} endY${endY}")
                // Step 2: Initialize MediaProjection and capture the screenshot
                initializeMediaProjection()
                // Notify the activity to pause the scanning effect
                val broadcastIntent = Intent("com.example.language_learning_helper.PAUSE_SCANNING").apply {
                }
                sendBroadcast(broadcastIntent)
                }

            if(intent.action == "com.example.language_learning_helper.TAKE_SCREENSHOT"){
                
                // Start the SelectionActivity and pass the values
                val selectionIntent = Intent(context, SelectionActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("startX", startX)
                    putExtra("startY", startY)
                    putExtra("endX", endX)
                    putExtra("endY", endY)
                }
                startActivity(selectionIntent)

                // Now, call captureScreenshot and pass the values to it
                captureScreenshot(startX, startY, endX, endY)
            }

            if(intent.action == "com.example.language_learning_helper.TOGGLE_FLOATING_ICON_VISIBILITY"){
                println("got broadcast of toggling floating icon vis in the service")
                
                try{
                servicePreferencesHelper.toggleShowFloatingIcon()
                val isFloatingIconVisible = servicePreferencesHelper.showFloatingIcon()
                val chatHeadLayoutParams = chatHeadView!!.layoutParams as WindowManager.LayoutParams
                if(isFloatingIconVisible){
                    chatHeadLayoutParams.x = 0
                    chatHeadLayoutParams.y = 100
                    windowManager!!.updateViewLayout(chatHeadView, chatHeadLayoutParams)

                    // Check if the app is in the foreground or background
                    if (!isAppInForeground()) {
                        // App is in the background, show the floating icon
                        val broadcastIntent = Intent("com.example.language_learning_helper.SHOW_FLOATING_ICON")
                        sendBroadcast(broadcastIntent)
                    }
                }else{
                    val broadcastIntent = Intent("com.example.language_learning_helper.HIDE_FLOATING_ICON")
                    sendBroadcast(broadcastIntent)
                }
                updateNotificationAction(context, isFloatingIconVisible)
                }catch(e: Exception){
                    println("${e.message}")
                }
            }

            if(intent.action == "com.example.language_learning_helper.STOP_SERVICE"){
                println("got broadcast of stopping the service in the service")
                stopService(Intent(this@FloatingIconService, FloatingIconService::class.java))
            }

            if(intent.action == "com.example.language_learning_helper.TOGGLE_SCANNING_MODE"){
                println("broadcast toggle scanning mode")
                servicePreferencesHelper.toggleScanningModeOn()
                isScanningModeOn = servicePreferencesHelper.isScanningModeOn()
            }

            if(intent.action == "com.example.language_learning_helper.STOP_SCANNING_BY_FORCE"){
                isCapturing = false
                println("stopped scanning by force")
            }

            if(intent.action == "com.example.language_learning_helper.HIDE_FLOATING_ICON"){
                println("hide")
                chatHeadView.visibility = View.GONE
                removeView.visibility = View.GONE
            } 

            if(intent.action == "com.example.language_learning_helper.SHOW_FLOATING_ICON"){
                chatHeadView.visibility = View.VISIBLE
            }            
        }
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcesses = activityManager.runningAppProcesses
    
        for (process in runningAppProcesses) {
            if (process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return process.processName == this.packageName
            }
        }
        return false
    }    


    companion object {
        private const val CHANNEL_ID = "floating_icon_service_channel"
        private const val NOTIFICATION_ID = 1
        var isCapturing = false
        var mediaProjectionData: Intent? = null
        var resultCode: Int = 0
        var isServiceRunning = false

        fun startService(context: Context) {
            val intent = Intent(context, FloatingIconService::class.java)
            context.startService(intent)
            isServiceRunning = true
        }   

        private fun updateNotificationAction(context: Context, isFloatingIconVisible: Boolean) {
            // Obtain the NotificationManager
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
            // Intent for "Open" action
            val openIntent = Intent("com.example.language_learning_helper.TOGGLE_FLOATING_ICON_VISIBILITY")
            val openPendingIntent = PendingIntent.getBroadcast(context, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)
        
            // Intent for "Turn Off" action
            val anotherActionIntent = Intent("com.example.language_learning_helper.STOP_SERVICE")
            val anotherActionPendingIntent = PendingIntent.getBroadcast(context, 1, anotherActionIntent, PendingIntent.FLAG_IMMUTABLE)
        
            val translationInput = ""
            // Intent for tapping the notification itself (when user taps on the notification)
            val tapIntent = Intent(context, TranslationWindowActivity::class.java).apply {
                putExtra("TRANSLATION_INPUT", translationInput)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required when launching from a service
            }
            val tapPendingIntent = PendingIntent.getActivity(context, 2, tapIntent, PendingIntent.FLAG_IMMUTABLE)

            // Determine the text based on the boolean
            val floatingIconActionText = if (isFloatingIconVisible) "Hide Floating Icon" else "Show Floating Icon"
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.owl_notification) // Use your icon here
                .setContentTitle("Quick tap translation is on")
                .setContentText("Tap to open the floating window.")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(tapPendingIntent)
                .addAction(0, "$floatingIconActionText", openPendingIntent)  // First button
                .addAction(0, "Turn Off", anotherActionPendingIntent)  // Second button
                .build()
            // Update the notification
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
        
    }

    override fun onCreate() {
        super.onCreate()
        
        // Register the BroadcastReceiver with an IntentFilter
        val filter = IntentFilter().apply {
            addAction("com.example.language_learning_helper.RENDER_COMPLETE")
            addAction("com.example.language_learning_helper.TAKE_SCREENSHOT")
            addAction("com.example.language_learning_helper.TOGGLE_FLOATING_ICON_VISIBILITY")
            addAction("com.example.language_learning_helper.STOP_SERVICE") 
            addAction("com.example.language_learning_helper.TOGGLE_SCANNING_MODE")
            addAction("com.example.language_learning_helper.STOP_SCANNING_BY_FORCE")
            addAction("com.example.language_learning_helper.HIDE_FLOATING_ICON")
            addAction("com.example.language_learning_helper.SHOW_FLOATING_ICON")
        }
    
        try {
            registerReceiver(recognitionReceiver, filter)
            println("BroadcastReceiver registered successfully in FloatingIconService")
            
            // Initialize SharedPreferences helper
            servicePreferencesHelper = ServiceSharedPreferencesHelper(this)
            // Create the notification channel and start the service in the foreground
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification(servicePreferencesHelper.showFloatingIcon()))
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to register BroadcastReceiver: ${e.message}")
        }

        val serviceStartedIntent = Intent("com.example.language_learning_helper.SERVICE_STARTED")
        sendBroadcast(serviceStartedIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handleStart()
        return START_STICKY
    }



    
    @SuppressLint("ClickableViewAccessibility")
    private fun handleStart() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        removeView = inflater.inflate(R.layout.remove, null) as RelativeLayout

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            WindowManager.LayoutParams.TYPE_PHONE;
        }

        paramRemove = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            flag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        paramRemove.gravity = Gravity.TOP or Gravity.LEFT
        removeView!!.visibility = View.GONE
        removeImg = removeView!!.findViewById<View>(R.id.remove_img) as? ImageView

        removeImg?.let { imageView -> // Work with the imageView only if it's not null
            try {
                val inputStream = assets.open("close.svg")
                val svg = SVG.getFromInputStream(inputStream)
                val drawable = PictureDrawable(svg.renderToPicture())
                imageView.setImageDrawable(drawable)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        windowManager!!.addView(removeView, paramRemove)
        
        chatHeadView = inflater.inflate(R.layout.chathead, null) as RelativeLayout
        chatHeadImg = chatHeadView!!.findViewById<View>(R.id.chathead_img) as? ImageView // Safe cast
        chatHeadView!!.visibility = View.GONE

        chatHeadImg?.let { imageView -> // Work with the imageView only if it's not null
            try {
                val inputStream = assets.open("owl.svg")
                val svg = SVG.getFromInputStream(inputStream)
                val drawable = PictureDrawable(svg.renderToPicture())
                imageView.setImageDrawable(drawable)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        windowManager!!.defaultDisplay.getSize(szWindow)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            flag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.LEFT
        params.x = 0
        params.y = 100

        isScanningModeOn = servicePreferencesHelper.isScanningModeOn()
        val gestureDetector = GestureDetector(this@FloatingIconService, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if(isScanningModeOn){
                    chatHeadClick()
                }else{
                    val translationInput = ""
                    val intent = Intent(this@FloatingIconService, TranslationWindowActivity::class.java).apply {
                        putExtra("TRANSLATION_INPUT", translationInput)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required when launching from a service
                    }
                    startActivity(intent)
                }
                return true
            }
        
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if(isScanningModeOn){
                    chatHeadDoubleClick()
                }
                return true
            }
        })

        chatHeadView!!.setOnTouchListener(object : View.OnTouchListener {
            var timeStart: Long = 0
            var timeEnd: Long = 0
            var isLongClick = false
            var inBounded = false
            var removeImgWidth = 0
            var removeImgHeight = 0
            var handlerLongClick = Handler()
            var runnableLongClick = Runnable {
                isLongClick = true
                if(chatHeadView.visibility == View.VISIBLE){
                    removeView!!.visibility = View.VISIBLE
                    chatHeadLongClick()
                }
            }
        
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if(event != null){
                    gestureDetector.onTouchEvent(event)
                }

                val layoutParams = chatHeadView!!.layoutParams as WindowManager.LayoutParams
                val xCord = event!!.rawX!!.toInt()
                val yCord = event!!.rawY!!.toInt()
                val xCordDestination: Int
                var yCordDestination: Int
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        timeStart = System.currentTimeMillis()
                        handlerLongClick.postDelayed(runnableLongClick, 600)
                        removeImgWidth = removeImg!!.layoutParams.width
                        removeImgHeight = removeImg!!.layoutParams.height
                        xInitCord = xCord
                        yInitCord = yCord
                        xInitMargin = layoutParams.x
                        yInitMargin = layoutParams.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val xDiffMove = xCord - xInitCord
                        val yDiffMove = yCord - yInitCord
                        xCordDestination = xInitMargin + xDiffMove
                        yCordDestination = yInitMargin + yDiffMove
                        if (isLongClick) {
                            val xBoundLeft = szWindow.x / 2 - (removeImgWidth * 1.5).toInt()
                            val xBoundRight = szWindow.x / 2 + (removeImgWidth * 1.5).toInt()
                            val yBoundTop = szWindow.y - (removeImgHeight * 1.5).toInt()
                            if (xCord >= xBoundLeft && xCord <= xBoundRight && yCord >= yBoundTop) {
                                inBounded = true
                                val xCordRemove = ((szWindow.x - removeImgHeight * 1.5) / 2).toInt()
                                val yCordRemove = (szWindow.y - (removeImgWidth * 1.5 + statusBarHeight)).toInt()
                                if (removeImg!!.layoutParams.height == removeImgHeight) {
                                    removeImg!!.layoutParams.height = (removeImgHeight * 1.5).toInt()
                                    removeImg!!.layoutParams.width = (removeImgWidth * 1.5).toInt()
                                    val paramRemove = removeView!!.layoutParams as WindowManager.LayoutParams
                                    paramRemove.x = xCordRemove
                                    paramRemove.y = yCordRemove
                                    windowManager!!.updateViewLayout(removeView, paramRemove)
                                }
                                layoutParams.x = xCordRemove + abs(removeView!!.width - chatHeadView!!.width) / 2
                                layoutParams.y = yCordRemove + abs(removeView!!.height - chatHeadView!!.height) / 2
                                windowManager!!.updateViewLayout(chatHeadView, layoutParams)
                            } else {
                                inBounded = false
                                removeImg!!.layoutParams.height = removeImgHeight
                                removeImg!!.layoutParams.width = removeImgWidth
                                val paramRemove = removeView!!.layoutParams as WindowManager.LayoutParams
                                val xCordRemove = (szWindow.x - removeView!!.width) / 2
                                val yCordRemove = szWindow.y - (removeView!!.height + statusBarHeight)
                                paramRemove.x = xCordRemove
                                paramRemove.y = yCordRemove
                                windowManager!!.updateViewLayout(removeView, paramRemove)
                            }
                        }
                        layoutParams.x = xCordDestination
                        layoutParams.y = yCordDestination
                        windowManager!!.updateViewLayout(chatHeadView, layoutParams)
                    }
                    MotionEvent.ACTION_UP -> {
                        isLongClick = false
                        removeView!!.visibility = View.GONE
                        removeImg!!.layoutParams.height = removeImgHeight
                        removeImg!!.layoutParams.width = removeImgWidth
                        handlerLongClick.removeCallbacks(runnableLongClick)
                        if (inBounded) {
                            val openIntent = Intent("com.example.language_learning_helper.TOGGLE_FLOATING_ICON_VISIBILITY")
                            sendBroadcast(openIntent)
                        }
                        if(!inBounded){
                            val xDiff = xCord - xInitCord
                            val yDiff = yCord - yInitCord
                            // if (abs(xDiff) < 5 && abs(yDiff) < 5) {
                            //     timeEnd = System.currentTimeMillis()
                            //     if (timeEnd - timeStart < 300) {
                            //         chatHeadClick()
                            //     }
                            // }
                            yCordDestination = yInitMargin + yDiff
                            val barHeight = statusBarHeight
                            if (yCordDestination < 0) {
                                yCordDestination = 0
                            } else if (yCordDestination + (chatHeadView!!.height + barHeight) > szWindow.y) {
                                yCordDestination = szWindow.y - (chatHeadView!!.height + barHeight)
                            }
                            layoutParams.y = yCordDestination
                            inBounded = false
                            resetPosition(xCord)
                        }else{
                            inBounded = false
                        }
                    }
                }
                return true
            }
        })
        val isFloatingIconVisible = servicePreferencesHelper.showFloatingIcon()
        if(!isFloatingIconVisible){
            chatHeadView.visibility = View.GONE
        }
        windowManager!!.addView(chatHeadView, params)
    }
    
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (windowManager == null) return
        windowManager!!.defaultDisplay.getSize(szWindow)
        val layoutParams = chatHeadView!!.layoutParams as WindowManager.LayoutParams
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (layoutParams.y + (chatHeadView!!.height + statusBarHeight) > szWindow.y) {
                layoutParams.y = szWindow.y - (chatHeadView!!.height + statusBarHeight)
                windowManager!!.updateViewLayout(chatHeadView, layoutParams)
            }
            if (layoutParams.x != 0 && layoutParams.x < szWindow.x) {
                resetPosition(szWindow.x)
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (layoutParams.x > szWindow.x) {
                resetPosition(szWindow.x)
            }
        }
        val chatHeadImageView = chatHeadView!!.findViewById<ImageView>(R.id.chathead_img)
        chatHeadImageView.background = null  // Clear existing background
        chatHeadImageView.setBackgroundResource(R.drawable.circle_background)  // Set new background
    }
    
    private fun resetPosition(x_cord_now: Int) {
        if ((x_cord_now - 50) <= szWindow.x / 2) {
            isLeft = true
            moveToLeft(x_cord_now)
        } else {
            isLeft = false
            moveToRight(x_cord_now)
        }
    }

    private fun moveToLeft(x_cord_now: Int) {
        object : CountDownTimer(500, 5) {
            var mParams = chatHeadView!!.layoutParams as WindowManager.LayoutParams
            override fun onTick(t: Long) {
                mParams.x = 0 // Move directly to the left edge
                windowManager!!.updateViewLayout(chatHeadView, mParams)
            }
    
            override fun onFinish() {
                mParams.x = 0 // Ensure the final position is at the left edge
                windowManager!!.updateViewLayout(chatHeadView, mParams)
            }
        }.start()
    }
    


    private fun moveToRight(x_cord_now: Int) {
        object : CountDownTimer(500, 5) {
            var mParams = chatHeadView!!.layoutParams as WindowManager.LayoutParams
            override fun onTick(t: Long) {
                mParams.x = szWindow.x - chatHeadView!!.width // Move directly to the right edge
                windowManager!!.updateViewLayout(chatHeadView, mParams)
            }
    
            override fun onFinish() {
                mParams.x = szWindow.x - chatHeadView!!.width // Ensure the final position is at the right edge
                windowManager!!.updateViewLayout(chatHeadView, mParams)
            }
        }.start()
    }
    

    private fun bounceValue(step: Long, scale: Long): Double {
        return scale * exp(-0.35 * step) * cos(0.1 * step)
    }


    private val statusBarHeight: Int
        get() = ceil(25 * applicationContext.resources.displayMetrics.density)
            .toInt()



    private fun chatHeadClick() {
        if (isCapturing) {
            Toast.makeText(this, "Scanning already in progress.", Toast.LENGTH_SHORT).show()
        }else{
            isCapturing = true
            val scanningIntent = Intent(this@FloatingIconService, ScanningEffectActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(scanningIntent)
        }
    }


    private fun chatHeadDoubleClick() {
        println("double clicked")
        if (isCapturing) {
            Toast.makeText(this, "Scanning already in progress.", Toast.LENGTH_SHORT).show()
        }else{
            isCapturing = true
            val rectangleIntent = Intent(this@FloatingIconService, RectangleSelectionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(rectangleIntent)
        }
    }


    private fun chatHeadLongClick() {
        val param_remove = removeView!!.layoutParams as WindowManager.LayoutParams
        val x_cord_remove = (szWindow.x - removeView!!.width) / 2
        val y_cord_remove = szWindow.y - (removeView!!.height + statusBarHeight)
        param_remove.x = x_cord_remove
        param_remove.y = y_cord_remove

        if (removeView!!.windowToken != null)
            windowManager!!.updateViewLayout(removeView, param_remove)
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

    

    private fun captureScreenshot(startX: Float, startY: Float, endX: Float, endY: Float) {
    
        try {
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getRealSize(size)
            val width = size.x
            val height = size.y
            val density = resources.displayMetrics.densityDpi
            // ImageReader to capture the screen
            val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
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
    
                        // Calculate StatusBar and NavBar heights explicitly
                        val statusBarHeight = resources.getIdentifier("status_bar_height", "dimen", "android").let { resId ->
                            if (resId > 0) resources.getDimensionPixelSize(resId) else 0
                        }
    
                        val navBarHeight = resources.getIdentifier("navigation_bar_height", "dimen", "android").let { resId ->
                            if (resId > 0) resources.getDimensionPixelSize(resId) else 0
                        }
    
                        println("StatusBarHeight=$statusBarHeight, NavBarHeight=$navBarHeight, Width=$width, Height=$height")
    
                        // Calculate the rectangle coordinates relative to the visible area
                        val rectStartX = (startX * width).toInt().coerceIn(0, width) // Convert startX to screen pixels
                        val rectStartY = (startY * (height - statusBarHeight - navBarHeight)).toInt().coerceIn(0, height - statusBarHeight - navBarHeight) // Convert startY to visible area pixels
                        val rectEndX = (endX * width).toInt().coerceIn(rectStartX, width) // Convert endX to screen pixels
                        val rectEndY = (endY * (height - statusBarHeight - navBarHeight)).toInt().coerceIn(rectStartY, height - statusBarHeight - navBarHeight) // Convert endY to visible area pixels
                        
                        println("${rectStartX}  ${rectStartY}  ${rectEndX}  ${rectEndY}")
                        // Crop the bitmap directly to the selected rectangle, adjusting for the status bar offset
                        try{
                        val selectedRectBitmap = Bitmap.createBitmap(
                            fullBitmap,
                            rectStartX,
                            statusBarHeight + rectStartY, // Adjust Y offset for status bar
                            rectEndX - rectStartX,
                            rectEndY - rectStartY
                        )
                        // Save the selected rectangle bitmap or perform further operations
                        // Example: Save the image
                        // saveImageToPublicDirectory(selectedRectBitmap)
                        // Notify the activity to resume the scanning effect
                    
                        SelectionActivity.bitmap = selectedRectBitmap
                        val broadcastIntent = Intent("com.example.language_learning_helper.RESUME_SCANNING")
                        sendBroadcast(broadcastIntent)}catch(e: Exception){
                            println("${e.message}")
                        }
                    }
                    reader.close()
                    stopScreenCapture()
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
    

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        servicePreferencesHelper.setServiceOn(false)
        println("${servicePreferencesHelper.isServiceOn()}")
        stopScreenCapture()
        unregisterReceiver(recognitionReceiver)

        // Safely remove `chatHeadView` if it exists and is attached to the window
        chatHeadView?.let {
            if (it.isAttachedToWindow) {
                windowManager?.removeView(it)
            }
        }

        // Safely remove `removeView` if it exists and is attached to the window
        removeView?.let {
            if (it.isAttachedToWindow) {
                windowManager?.removeView(it)
            }
        }
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

    private fun createNotification(isFloatingIconVisible: Boolean): Notification {
        // Intent for "Open" action
        val openIntent = Intent("com.example.language_learning_helper.TOGGLE_FLOATING_ICON_VISIBILITY")
        val openPendingIntent = PendingIntent.getBroadcast(this@FloatingIconService, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)
    
        // Intent for "Turn Off" action
        val anotherActionIntent = Intent("com.example.language_learning_helper.STOP_SERVICE")
        val anotherActionPendingIntent = PendingIntent.getBroadcast(this@FloatingIconService, 1, anotherActionIntent, PendingIntent.FLAG_IMMUTABLE)
    
        val translationInput = ""
        // Intent for tapping the notification itself (when user taps on the notification)
        val tapIntent = Intent(this@FloatingIconService, TranslationWindowActivity::class.java).apply {
            putExtra("TRANSLATION_INPUT", translationInput)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required when launching from a service
        }
        val tapPendingIntent = PendingIntent.getActivity(this@FloatingIconService, 2, tapIntent, PendingIntent.FLAG_IMMUTABLE)

        // Determine the text based on the boolean
        val floatingIconActionText = if (isFloatingIconVisible) "Hide Floating Icon" else "Show Floating Icon"
        
        return NotificationCompat.Builder(this@FloatingIconService, CHANNEL_ID)
            .setSmallIcon(R.drawable.owl_notification) // Use your icon here
            .setContentTitle("Quick tap translation is on")
            .setContentText("Tap to open the floating window.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(tapPendingIntent)
            .addAction(0, "$floatingIconActionText", openPendingIntent)  // First button
            .addAction(0, "Turn Off", anotherActionPendingIntent)  // Second button
            .build()
    }
    


    private fun stopScreenCapture() {
        if(virtualDisplay != null && mediaProjection != null){
            virtualDisplay?.release()
            mediaProjection?.stop()
        }
        virtualDisplay = null
        mediaProjection = null
    }
}
