package com.paylisher

import com.paylisher.internal.PaylisherContext
import com.paylisher.internal.PaylisherDateProvider
import com.paylisher.internal.PaylisherDeviceDateProvider
import com.paylisher.internal.PaylisherLogger
import com.paylisher.internal.PaylisherNetworkStatus
import com.paylisher.internal.PaylisherNoOpLogger
import com.paylisher.internal.PaylisherPreferences
import com.paylisher.internal.PaylisherSerializer
import java.util.UUID

/**
 * The SDK Config
 */
public open class PaylisherConfig(
    /**
     * The Paylisher API Key
     */
    public val apiKey: String,
    /**
     * The Paylisher Host
     *
     */
    public val host: String = DEFAULT_HOST,
    /**
     * Logs the debug logs to the [logger] if enabled
     * Defaults to false
     */
    public var debug: Boolean = false,
    /**
     * This flag prevents capturing any data if enabled
     * You can overwrite this value at runtime by calling [Paylisher.optIn()]] or Paylisher.optOut()
     * Defaults to false
     */
    @Volatile
    public var optOut: Boolean = false,
    /**
     * Send a $feature_flag_called event when a feature flag is used automatically
     * Used by experiments
     *
     * Defaults to true
     */
    public var sendFeatureFlagEvent: Boolean = true,
    /**
     * Preload feature flags automatically
     * Docs https://paylisher.com/docs/feature-flags and https://paylisher.com/docs/experiments
     * Defaults to true
     */
    public var preloadFeatureFlags: Boolean = true,
    /**
     * Number of minimum events before they are sent over the wire
     * Defaults to 20
     */
    public var flushAt: Int = 20,
    /**
     * Number of maximum events in memory and disk, when the maximum is exceed, the oldest
     * event is deleted and the new one takes place
     * Defaults to 1000
     */
    public var maxQueueSize: Int = 1000,
    /**
     * Number of maximum events in a batch call
     * Defaults to 50
     */
    public var maxBatchSize: Int = 50,
    // (30).toDuration(DurationUnit.SECONDS) requires Kotlin 1.6
    /**
     * Interval in seconds for sending events over the wire
     * The lower the number, most likely more battery is used
     * Defaults to 30s
     */
    @Suppress("ktlint:standard:no-consecutive-comments")
    public var flushIntervalSeconds: Int = 30,
    /**
     * Hook for encrypt and decrypt events
     * Devices are sandbox so likely not needed
     * Defaults to no encryption
     */
    public var encryption: PaylisherEncryption? = null,
    /**
     * Hook that is called when feature flags are loaded
     * Defaults to no callback
     */
    public var onFeatureFlags: PaylisherOnFeatureFlags? = null,
    /**
     * Enable Recording of Session Replays for Android
     * Requires Record user sessions to be enabled in the Paylisher Project Settings
     * Defaults to false
     */
    @PaylisherExperimental
    public var sessionReplay: Boolean = false,
    /**
     * Hook that allows to sanitize the event properties
     * The hook is called before the event is cached or sent over the wire
     */
    public var propertiesSanitizer: PaylisherPropertiesSanitizer? = null,
    /**
     * Hook that allows for modification of the default mechanism for
     * generating anonymous id (which as of now is just random UUID v7)
     */
    public var getAnonymousId: ((UUID) -> UUID) = { it },
    /**
     * Determines the behavior for processing user profiles.
     * - `ALWAYS`: We will process persons data for all events.
     * - `NEVER`: Never processes user profile data. This means that anonymous users will not be merged when they sign up or log in.
     * - `IDENTIFIED_ONLY` (default): we will only process persons when you call `identify`, `alias`, and `group`, Anonymous users won't get person profiles.
     */
    public var personProfiles: PersonProfiles = PersonProfiles.IDENTIFIED_ONLY,
) {
    @PaylisherInternal
    public var logger: PaylisherLogger = PaylisherNoOpLogger()

    @PaylisherInternal
    public val serializer: PaylisherSerializer by lazy {
        PaylisherSerializer(this)
    }

    @PaylisherInternal
    public var context: PaylisherContext? = null

    @PaylisherInternal
    public var sdkName: String = "paylisher-java"

    @PaylisherInternal
//    public var sdkVersion: String = "24.10.0"
    public var sdkVersion: String = SDK_VERSION
//    public var sdkVersion: String = BuildConfig.VERSION_NAME

    internal val userAgent: String
        get() {
            return "$sdkName/$sdkVersion"
        }

    @PaylisherInternal
    public var legacyStoragePrefix: String? = null

    @PaylisherInternal
    public var storagePrefix: String? = null

    @PaylisherInternal
    public var replayStoragePrefix: String? = null

    @PaylisherInternal
    public var errorStoragePrefix: String? = null

    @PaylisherInternal
    public var cachePreferences: PaylisherPreferences? = null

    @PaylisherInternal
    public var networkStatus: PaylisherNetworkStatus? = null

    @PaylisherInternal
    public var snapshotEndpoint: String = "/s/"

    @PaylisherInternal
    public var dateProvider: PaylisherDateProvider = PaylisherDeviceDateProvider()

    private val integrationsList: MutableList<PaylisherIntegration> = mutableListOf()
    private val integrationLock = Any()

    /**
     * The integrations list
     */
    public val integrations: List<PaylisherIntegration>
        get() {
            val list: List<PaylisherIntegration>
            synchronized(integrationLock) {
                list = integrationsList.toList()
            }
            return list
        }

    /**
     * Adds a new integration
     * @param integration the Integration
     */
    public fun addIntegration(integration: PaylisherIntegration) {
        synchronized(integrationLock) {
            integrationsList.add(integration)
        }
    }

    /**
     * Removes the integration
     * @param integration the Integration
     */
    public fun removeIntegration(integration: PaylisherIntegration) {
        synchronized(integrationLock) {
            integrationsList.remove(integration)
        }
    }

    public companion object {
        public const val DEFAULT_HOST: String = "https://datastudio.paylisher.com" // TODO: check later
    }
}
