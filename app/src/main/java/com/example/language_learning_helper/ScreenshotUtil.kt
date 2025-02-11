// package com.example.language_learning_helper

// import android.app.Activity
// import android.graphics.Bitmap
// import android.os.Build
// import android.view.PixelCopy
// import android.view.View
// import android.widget.Toast
// import android.os.Handler
// import android.os.Looper
// import android.graphics.Canvas

// object ScreenshotUtil {

//     fun captureScreenshot(activity: Activity): Bitmap {
//         val rootView = activity.window.decorView.rootView

//         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//             val window = activity.window
//             val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)

//             // PixelCopy requires a valid handler for its callbacks
//             PixelCopy.request(
//                 window,
//                 bitmap,
//                 { copyResult ->
//                     if (copyResult != PixelCopy.SUCCESS) {
//                         Toast.makeText(activity, "Screenshot failed with result: $copyResult", Toast.LENGTH_SHORT).show()
//                     }
//                 },
//                 Handler(Looper.getMainLooper()) // Provide a handler to run on the main thread
//             )
//             return bitmap
//         } else {
//             // Fallback for API < 26
//             val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
//             val canvas = Canvas(bitmap)
//             rootView.draw(canvas)
//             return bitmap
//         }
//     }
// }
