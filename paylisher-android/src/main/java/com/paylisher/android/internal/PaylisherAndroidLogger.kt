package com.paylisher.android.internal

import android.util.Log
import com.paylisher.android.PaylisherAndroidConfig
import com.paylisher.internal.PaylisherLogger

/**
 * Logs the messages using Logcat only if config.debug is enabled
 * @property config the Config
 */
internal class PaylisherAndroidLogger(private val config: PaylisherAndroidConfig) : PaylisherLogger {
    override fun log(message: String) {
        if (isEnabled()) {
            Log.println(Log.DEBUG, "Paylisher", message)
        }
    }

    override fun isEnabled(): Boolean {
        return config.debug
    }
}
