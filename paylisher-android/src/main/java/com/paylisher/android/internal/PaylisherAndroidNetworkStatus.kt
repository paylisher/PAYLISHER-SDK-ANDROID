package com.paylisher.android.internal

import android.content.Context
import com.paylisher.internal.PaylisherNetworkStatus

/**
 * Checks if there's an active network enabled
 * @property context the Config
 */
internal class PaylisherAndroidNetworkStatus(private val context: Context) : PaylisherNetworkStatus {
    override fun isConnected(): Boolean {
        return context.isConnected()
    }
}
