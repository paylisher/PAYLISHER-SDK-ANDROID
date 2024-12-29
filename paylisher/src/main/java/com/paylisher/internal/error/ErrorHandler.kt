package com.paylisher.internal.error

import com.paylisher.internal.PaylisherApiError

internal interface ErrorHandler {
    fun handleError(exception: Exception, context: String)
    fun handleError(throwable: Throwable, thread: Thread)
    fun handleError(error: PaylisherApiError, context: String)
}
