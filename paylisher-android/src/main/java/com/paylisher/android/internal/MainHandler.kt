package com.paylisher.android.internal

import android.os.Handler
import android.os.Looper
import com.paylisher.PaylisherInternal

@PaylisherInternal
public class MainHandler(public val mainLooper: Looper = Looper.getMainLooper()) {
    public val handler: Handler = Handler(mainLooper)
}
