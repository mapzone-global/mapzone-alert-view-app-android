package com.mapzone.mapzonealertview.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.createBitmap

internal fun drawableToBitmap(
    context: Context,
    @DrawableRes resId: Int,
    targetSizeDp: Float? = 48f,
): Bitmap? {
    val drawable = AppCompatResources.getDrawable(context, resId) ?: return null
    val iw = drawable.intrinsicWidth.coerceAtLeast(1)
    val ih = drawable.intrinsicHeight.coerceAtLeast(1)

    val (w, h) = if (targetSizeDp == null) {
        iw to ih
    } else {
        val targetPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            targetSizeDp,
            context.resources.displayMetrics,
        ).toInt().coerceAtLeast(1)
        if (iw >= ih) {
            targetPx to (targetPx * ih / iw).coerceAtLeast(1)
        } else {
            (targetPx * iw / ih).coerceAtLeast(1) to targetPx
        }
    }

    val bmp = createBitmap(w, h)
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, w, h)
    drawable.draw(canvas)
    return bmp
}
