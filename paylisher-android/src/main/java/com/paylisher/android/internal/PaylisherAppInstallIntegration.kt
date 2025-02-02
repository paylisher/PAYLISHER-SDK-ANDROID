package com.paylisher.android.internal

import android.content.Context
import com.paylisher.Paylisher
import com.paylisher.PaylisherIntegration
import com.paylisher.android.PaylisherAndroidConfig
import com.paylisher.internal.PaylisherPreferences.Companion.BUILD
import com.paylisher.internal.PaylisherPreferences.Companion.VERSION

/**
 * Captures app installed and updated events
 * @property context the App Context
 * @property config the Config
 */
internal class PaylisherAppInstallIntegration(
    private val context: Context,
    private val config: PaylisherAndroidConfig,
) : PaylisherIntegration {
    override fun install() {
        getPackageInfo(context, config)?.let { packageInfo ->
            config.cachePreferences?.let { preferences ->
                val versionName = packageInfo.versionName
                val versionCode = packageInfo.versionCodeCompat()

                val previousVersion = preferences.getValue(VERSION) as? String
                var previousBuild = preferences.getValue(BUILD)

                val event: String
                val props = mutableMapOf<String, Any>()
                if (previousBuild == null) {
                    event = "Application Installed"
                } else {
                    // to keep compatibility
                    if (previousBuild is Int) {
                        previousBuild = previousBuild.toLong()
                    }

                    // Do not send version updates if its the same
                    if (previousBuild == versionCode) {
                        return
                    }

                    event = "Application Updated"
                    previousVersion?.let {
                        props["previous_version"] = it
                    }
                    props["previous_build"] = previousBuild
                }
                props["version"] = versionName
                props["build"] = versionCode

                preferences.setValue(VERSION, versionName)
                preferences.setValue(BUILD, versionCode)

                Paylisher.capture(event, properties = props)
            }
        }
    }
}
