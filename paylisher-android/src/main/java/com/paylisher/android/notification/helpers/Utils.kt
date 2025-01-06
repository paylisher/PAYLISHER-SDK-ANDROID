package com.paylisher.android.notification.helpers

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.RelativeLayout
import com.paylisher.android.R
import com.paylisher.android.notification.InAppMessagingLayout.CloseIcon
import com.paylisher.android.notification.InAppMessagingLayout.CloseText
import com.paylisher.android.notification.helpers.InAppLocalize.Companion.localize

class Utils {
    companion object {

        fun applyRoundedCornerMask(bitmap: Bitmap, radius: Float): Bitmap {
            // Create a new bitmap with the same width and height as the original image
            val roundedBitmap =
                Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(roundedBitmap)

            // Set up the paint object to apply anti-aliasing for smooth edges
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Create a rounded rectangle that will act as the mask for the bitmap
            val rectF = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())

            // Set up the paint to use the SRC_IN mode, so the image will only be drawn inside the rounded rectangle
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

            // Draw the rounded rectangle (this will be the mask)
            canvas.drawRoundRect(rectF, radius, radius, paint)

            // Now, draw the original bitmap on the canvas (it will be clipped by the rounded rectangle)
            canvas.drawBitmap(bitmap, 0f, 0f, null)

            return roundedBitmap
        }

    }

}