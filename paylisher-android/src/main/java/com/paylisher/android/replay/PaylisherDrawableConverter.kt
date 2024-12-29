package com.paylisher.android.replay

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

public fun interface PaylisherDrawableConverter {
    public fun convert(drawable: Drawable): Bitmap?
}
