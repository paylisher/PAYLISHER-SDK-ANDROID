// Inspired from https://github.com/square/curtains/blob/487bda6de00638c6decb3394b8a50bf83bed7496/curtains/src/main/java/curtains/internal/NextDrawListener.kt#L13

package com.paylisher.android.replay.internal

import android.view.View
import android.view.ViewTreeObserver
import com.paylisher.android.internal.MainHandler
import com.paylisher.internal.PaylisherDateProvider

internal class NextDrawListener(
    private val view: View,
    mainHandler: MainHandler,
    dateProvider: PaylisherDateProvider,
    debouncerDelayMs: Long,
    private val onDrawCallback: () -> Unit,
) : ViewTreeObserver.OnDrawListener {
    private val debounce = Debouncer(mainHandler, dateProvider, debouncerDelayMs)

    override fun onDraw() {
        debounce.debounce {
            onDrawCallback()
        }
    }

    private fun safelyRegisterForNextDraw() {
        if (view.isAlive()) {
            view.viewTreeObserver?.addOnDrawListener(this)
        }
    }

    internal companion object {
        // only call if onDecorViewReady
        internal fun View.onNextDraw(
            mainHandler: MainHandler,
            dateProvider: PaylisherDateProvider,
            debouncerDelayMs: Long,
            onDrawCallback: () -> Unit,
        ): NextDrawListener {
            val nextDrawListener = NextDrawListener(this, mainHandler, dateProvider, debouncerDelayMs, onDrawCallback)
            nextDrawListener.safelyRegisterForNextDraw()
            return nextDrawListener
        }
    }
}

internal fun View.isAliveAndAttachedToWindow(): Boolean {
    // Prior to API 26, OnDrawListener wasn't merged back from the floating ViewTreeObserver into
    // the real ViewTreeObserver.
    // https://android.googlesource.com/platform/frameworks/base/+/9f8ec54244a5e0343b9748db3329733f259604f3
    return isAlive() && isAttachedToWindow
}

internal fun View.isAlive(): Boolean {
    return viewTreeObserver?.isAlive == true
}
