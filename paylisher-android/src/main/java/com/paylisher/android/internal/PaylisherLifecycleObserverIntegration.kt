package com.paylisher.android.internal

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.paylisher.Paylisher
import com.paylisher.PaylisherIntegration
import com.paylisher.android.PaylisherAndroidConfig
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicLong

/**
 * Captures app opened and backgrounded events
 * @property context the App Context
 * @property config the Config
 * @property lifecycle The Lifecycle, defaults to ProcessLifecycleOwner.get().lifecycle
 */
internal class PaylisherLifecycleObserverIntegration(
    private val context: Context,
    private val config: PaylisherAndroidConfig,
    private val mainHandler: MainHandler,
    private val lifecycle: Lifecycle = ProcessLifecycleOwner.get().lifecycle,
) : DefaultLifecycleObserver, PaylisherIntegration {
    private val timerLock = Any()
    private var timer = Timer(true)
    private var timerTask: TimerTask? = null
    private val lastUpdatedSession = AtomicLong(0L)
    private val sessionMaxInterval = (1000 * 60 * 30).toLong() // 30 minutes

    private companion object {
        // in case there are multiple instances or the SDK is closed/setup again
        // the value is still cached
        @JvmStatic
        @Volatile
        private var fromBackground = false
    }

    override fun onStart(owner: LifecycleOwner) {
        startSession()

        if (config.captureApplicationLifecycleEvents) {
            val props = mutableMapOf<String, Any>()
            props["from_background"] = fromBackground

            if (!fromBackground) {
                getPackageInfo(context, config)?.let { packageInfo ->
                    props["version"] = packageInfo.versionName
                    props["build"] = packageInfo.versionCodeCompat()
                }

                fromBackground = true
            }

            Paylisher.capture("Application Opened", properties = props)
        }
    }

    private fun startSession() {
        cancelTask()

        val currentTimeMillis = config.dateProvider.currentTimeMillis()
        val lastUpdatedSession = lastUpdatedSession.get()

        if (lastUpdatedSession == 0L ||
            (lastUpdatedSession + sessionMaxInterval) <= currentTimeMillis
        ) {
            Paylisher.startSession()
        }
        this.lastUpdatedSession.set(currentTimeMillis)
    }

    private fun cancelTask() {
        synchronized(timerLock) {
            timerTask?.cancel()
            timerTask = null
        }
    }

    private fun scheduleEndSession() {
        synchronized(timerLock) {
            cancelTask()
            timerTask =
                object : TimerTask() {
                    override fun run() {
                        Paylisher.endSession()
                    }
                }
            timer.schedule(timerTask, sessionMaxInterval)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (config.captureApplicationLifecycleEvents) {
            Paylisher.capture("Application Backgrounded")
        }

        val currentTimeMillis = config.dateProvider.currentTimeMillis()
        lastUpdatedSession.set(currentTimeMillis)
        scheduleEndSession()
    }

    private fun add() {
        lifecycle.addObserver(this)
    }

    override fun install() {
        try {
            if (isMainThread(mainHandler)) {
                add()
            } else {
                mainHandler.handler.post {
                    add()
                }
            }
        } catch (e: Throwable) {
            config.logger.log("Failed to install PaylisherLifecycleObserverIntegration: $e")
        }
    }

    private fun remove() {
        lifecycle.removeObserver(this)
    }

    override fun uninstall() {
        try {
            if (isMainThread(mainHandler)) {
                remove()
            } else {
                mainHandler.handler.post {
                    remove()
                }
            }
        } catch (e: Throwable) {
            config.logger.log("Failed to uninstall PaylisherLifecycleObserverIntegration: $e")
        }
    }
}
