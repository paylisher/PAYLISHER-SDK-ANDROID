package com.paylisher.internal

import com.paylisher.PaylisherConfig
import com.paylisher.PaylisherInternal
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.Executor

private fun isRequestCanceled(throwable: Throwable): Boolean {
    return throwable is IOException &&
        throwable.message?.contentEquals("Canceled") ?: false || throwable is InterruptedIOException
}

private fun noInternetAvailable(throwable: Throwable): Boolean {
    return throwable is UnknownHostException
}

private fun isConnectionTimeout(throwable: Throwable): Boolean {
    return throwable is SocketTimeoutException
}

internal fun Throwable.isNetworkingError(): Boolean {
    return isConnectionTimeout(this) ||
        noInternetAvailable(this) ||
        isRequestCanceled(this)
}

internal fun File.deleteSafely(config: PaylisherConfig) {
    try {
        delete()
    } catch (e: Throwable) {
        config.logger.log("Error deleting the file $name: $e.")
    }
}

internal fun File.existsSafely(config: PaylisherConfig): Boolean {
    return try {
        exists()
    } catch (e: Throwable) {
        config.logger.log("Error deleting the file $name: $e.")
        false
    }
}

internal fun Executor.executeSafely(run: Runnable) {
    try {
        // can throw RejectedExecutionException
        execute(run)
    } catch (ignored: Throwable) {
    }
}

@PaylisherInternal
public fun Thread.interruptSafely() {
    try {
        interrupt()
    } catch (e: Throwable) {
        // ignore
    }
}
