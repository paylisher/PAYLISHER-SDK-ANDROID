package com.paylisher.android.internal

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.paylisher.Paylisher
import com.paylisher.PaylisherIntegration
import com.paylisher.android.PaylisherAndroidConfig

/**
 * Captures deep link and screen view events
 * @property application the App Context
 * @property config the Config
 */
internal class PaylisherActivityLifecycleCallbackIntegration(
    private val application: Application,
    private val config: PaylisherAndroidConfig,
) : ActivityLifecycleCallbacks, PaylisherIntegration {
    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        if (config.captureDeepLinks) {
            activity.intent?.let { intent ->
                val props = mutableMapOf<String, Any>()
                val data = intent.data
                try {
                    data?.let {
                        for (item in it.queryParameterNames) {
                            val param = it.getQueryParameter(item)
                            if (!param.isNullOrEmpty()) {
                                props[item] = param
                            }
                        }
                    }
                } catch (e: UnsupportedOperationException) {
                    config.logger.log("Deep link $data has invalid query param names.")
                } finally {
                    data?.let { props["url"] = it.toString() }
                    intent.getReferrerInfo(config).let { props.putAll(it) }
                    Paylisher.capture("Deep Link Opened", properties = props)
                }
            }
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (config.captureScreenViews) {
            val screenName = activity.activityLabelOrName(config)

            if (!screenName.isNullOrEmpty()) {
                Paylisher.screen(screenName)
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun install() {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun uninstall() {
        application.unregisterActivityLifecycleCallbacks(this)
    }
}
