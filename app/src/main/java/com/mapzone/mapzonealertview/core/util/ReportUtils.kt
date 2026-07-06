package com.mapzone.mapzonealertview.core.util

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.PixelCopy
import android.view.View
import androidx.core.graphics.createBitmap
import vn.vietmap.vietmapsdk.maps.VietMapGL

object ReportUtils {
    private const val TAG = "ReportUtils"
    private const val ALBUM = "AlertViewReports"

    fun findActivity(context: Context): Activity? {
        var ctx: Context = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun coordFileName(lat: Double, lng: Double): String =
        "%.6f,%.6f".format(java.util.Locale.US, lat, lng)

    fun captureAndSave(
        activity: Activity,
        map: VietMapGL?,
        mapView: View?,
        fileName: String,
        onDone: (String?) -> Unit,
    ) {
        val root: View? = activity.findViewById(android.R.id.content)
        if (map == null || mapView == null || root == null || root.width <= 0 || root.height <= 0) {
            captureWindow(activity, fileName, onDone)
            return
        }
        val main = Handler(Looper.getMainLooper())
        try {
            val w = root.width
            val h = root.height
            val overlay = createBitmap(w, h)
            val prevVisibility = mapView.visibility
            mapView.visibility = View.INVISIBLE
            root.draw(Canvas(overlay))
            mapView.visibility = prevVisibility

            val rootLoc = IntArray(2).also { root.getLocationInWindow(it) }
            val mapLoc = IntArray(2).also { mapView.getLocationInWindow(it) }
            val dst = Rect(
                mapLoc[0] - rootLoc[0],
                mapLoc[1] - rootLoc[1],
                mapLoc[0] - rootLoc[0] + mapView.width,
                mapLoc[1] - rootLoc[1] + mapView.height,
            )

            map.snapshot { mapBmp ->
                Thread {
                    val saved = runCatching {
                        val full = createBitmap(w, h)
                        Canvas(full).apply {
                            drawBitmap(mapBmp, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
                            drawBitmap(overlay, 0f, 0f, null)
                        }
                        save(activity, full, fileName)
                    }.getOrElse {
                        Log.e(TAG, "Ghép ảnh lỗi", it)
                        null
                    }
                    main.post { onDone(saved) }
                }.start()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Chụp màn hình lỗi", t)
            onDone(null)
        }
    }

    private fun captureWindow(activity: Activity, fileName: String, onDone: (String?) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onDone(null)
            return
        }
        val window = activity.window
        val decor = window.decorView
        val w = decor.width
        val h = decor.height
        if (w <= 0 || h <= 0) {
            onDone(null)
            return
        }
        val bitmap = createBitmap(w, h)
        val loc = IntArray(2).also { decor.getLocationInWindow(it) }
        val rect = Rect(loc[0], loc[1], loc[0] + w, loc[1] + h)
        val main = Handler(Looper.getMainLooper())
        try {
            PixelCopy.request(window, rect, bitmap, { result ->
                if (result == PixelCopy.SUCCESS) {
                    Thread {
                        val saved = save(activity, bitmap, fileName)
                        main.post { onDone(saved) }
                    }.start()
                } else {
                    onDone(null)
                }
            }, main)
        } catch (t: Throwable) {
            Log.e(TAG, "Chụp màn hình lỗi", t)
            onDone(null)
        }
    }

    private fun save(context: Context, bitmap: Bitmap, fileName: String): String? {
        val name = "$fileName.png"
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$ALBUM")
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return null
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: return null
            "Pictures/$ALBUM/$name"
        } catch (t: Throwable) {
            Log.e(TAG, "Lưu report lỗi", t)
            null
        }
    }
}
