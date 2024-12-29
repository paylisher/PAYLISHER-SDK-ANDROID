package com.paylisher

import com.paylisher.internal.PaylisherApi
import com.paylisher.internal.PaylisherApiEndpoint
import com.paylisher.internal.PaylisherFeatureFlags
import com.paylisher.internal.PaylisherMemoryPreferences
import com.paylisher.internal.PaylisherNoOpLogger
import com.paylisher.internal.PaylisherPreferences
import com.paylisher.internal.PaylisherPreferences.Companion.ALL_INTERNAL_KEYS
import com.paylisher.internal.PaylisherPreferences.Companion.ANONYMOUS_ID
import com.paylisher.internal.PaylisherPreferences.Companion.BUILD
import com.paylisher.internal.PaylisherPreferences.Companion.DISTINCT_ID
import com.paylisher.internal.PaylisherPreferences.Companion.GROUPS
import com.paylisher.internal.PaylisherPreferences.Companion.IS_IDENTIFIED
import com.paylisher.internal.PaylisherPreferences.Companion.OPT_OUT
import com.paylisher.internal.PaylisherPreferences.Companion.PERSON_PROCESSING
import com.paylisher.internal.PaylisherPreferences.Companion.VERSION
import com.paylisher.internal.PaylisherPrintLogger
import com.paylisher.internal.PaylisherQueue
import com.paylisher.internal.PaylisherSendCachedEventsIntegration
import com.paylisher.internal.PaylisherSerializer
import com.paylisher.internal.PaylisherSessionManager
import com.paylisher.internal.PaylisherThreadFactory
import com.paylisher.internal.error.DefaultErrorHandler
import com.paylisher.vendor.uuid.TimeBasedEpochGenerator
import kotlinx.coroutines.runBlocking
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

public class Paylisher private constructor(
    private val queueExecutor: ExecutorService =
        Executors.newSingleThreadScheduledExecutor(
            PaylisherThreadFactory("PaylisherQueueThread"),
        ),
    private val replayExecutor: ExecutorService =
        Executors.newSingleThreadScheduledExecutor(
            PaylisherThreadFactory("PaylisherReplayQueueThread"),
        ),
    private val errorExecutor: ExecutorService =
        Executors.newSingleThreadScheduledExecutor(
            PaylisherThreadFactory("PaylisherErrorQueueThread"),
        ),
    private val featureFlagsExecutor: ExecutorService =
        Executors.newSingleThreadScheduledExecutor(
            PaylisherThreadFactory("PaylisherFeatureFlagsThread"),
        ),
    private val cachedEventsExecutor: ExecutorService =
        Executors.newSingleThreadScheduledExecutor(
            PaylisherThreadFactory("PaylisherSendCachedEventsThread"),
        ),
    private val reloadFeatureFlags: Boolean = true,
) : PaylisherInterface {
    @Volatile
    private var enabled = false

    private val setupLock = Any()
    private val optOutLock = Any()
    private val anonymousLock = Any()
    private val identifiedLock = Any()
    private val personProcessingLock = Any()
    private val groupsLock = Any()

    private val featureFlagsCalledLock = Any()

    private var config: PaylisherConfig? = null

    private var featureFlags: PaylisherFeatureFlags? = null
    private var queue: PaylisherQueue? = null
    private var replayQueue: PaylisherQueue? = null
    private var errorQueue: PaylisherQueue? = null
    private var memoryPreferences = PaylisherMemoryPreferences()
    private val featureFlagsCalled = mutableMapOf<String, MutableList<Any?>>()

    private var isIdentifiedLoaded: Boolean = false
    private var isPersonProcessingLoaded: Boolean = false

    public override fun <T : PaylisherConfig> setup(config: T) {
        synchronized(setupLock) {
            try {
                if (enabled) {
                    config.logger.log("Setup called despite already being setup!")
                    return
                }
                config.logger =
                    if (config.logger is PaylisherNoOpLogger) PaylisherPrintLogger(config) else config.logger

                if (!apiKeys.add(config.apiKey)) {
                    config.logger.log("API Key: ${config.apiKey} already has a Paylisher instance.")
                }

                val cachePreferences = config.cachePreferences ?: memoryPreferences
                config.cachePreferences = cachePreferences
                val errorHandler = DefaultErrorHandler(config)

                val api = PaylisherApi(config, errorHandler)
                val queue = PaylisherQueue(
                    config,
                    api,
                    PaylisherApiEndpoint.BATCH,
                    config.storagePrefix,
                    queueExecutor
                )
                val replayQueue = PaylisherQueue(
                    config,
                    api,
                    PaylisherApiEndpoint.SNAPSHOT,
                    config.replayStoragePrefix,
                    replayExecutor
                )
                val errorQueue = PaylisherQueue(
                    config,
                    api,
                    PaylisherApiEndpoint.ERROR,
                    config.errorStoragePrefix,
                    errorExecutor
                )
                val featureFlags = PaylisherFeatureFlags(config, api, featureFlagsExecutor)

                Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                    // Log the uncaught exception
                    config.logger.log("GlobalExceptionHandler " + "Uncaught exception in thread ${thread.name}: ${throwable.message}")

                    // Handle the exception, using safe casting
                    if (throwable != null) {
                        errorHandler.handleError(throwable, thread)
                    }
                }

                // no need to lock optOut here since the setup is locked already
                val optOut =
                    getPreferences().getValue(
                        OPT_OUT,
                        defaultValue = config.optOut,
                    ) as? Boolean
                optOut?.let {
                    config.optOut = optOut
                }

                val startDate = config.dateProvider.currentDate()
                val sendCachedEventsIntegration =
                    PaylisherSendCachedEventsIntegration(
                        config,
                        api,
                        startDate,
                        cachedEventsExecutor,
                    )

                this.config = config
                this.queue = queue
                this.replayQueue = replayQueue
                this.errorQueue = errorQueue
                this.featureFlags = featureFlags

                config.addIntegration(sendCachedEventsIntegration)

                legacyPreferences(config, config.serializer)

                enabled = true

                queue.start()

                startSession()

                config.integrations.forEach {
                    try {
                        it.install()
                    } catch (e: Throwable) {
                        config.logger.log("Integration ${it.javaClass.name} failed to install: $e.")
                    }
                }

                // only because of testing in isolation, this flag is always enabled
                if (reloadFeatureFlags && config.preloadFeatureFlags) {
                    loadFeatureFlagsRequest(config.onFeatureFlags)
                }
            } catch (e: Throwable) {
                config.logger.log("Setup failed: $e.")
            }
        }
    }

    private fun getPreferences(): PaylisherPreferences {
        return config?.cachePreferences ?: memoryPreferences
    }

    private fun legacyPreferences(
        config: PaylisherConfig,
        serializer: PaylisherSerializer,
    ) {
        val cachedPrefs = getPreferences().getValue(config.apiKey) as? String
        cachedPrefs?.let {
            try {
                serializer.deserialize<Map<String, Any>?>(it.reader())?.let { props ->
                    val anonymousId = props["anonymousId"] as? String
                    val distinctId = props["distinctId"] as? String

                    if (!anonymousId.isNullOrBlank()) {
                        this.anonymousId = anonymousId
                    }
                    if (!distinctId.isNullOrBlank()) {
                        this.distinctId = distinctId
                    }

                    getPreferences().remove(config.apiKey)
                }
            } catch (e: Throwable) {
                config.logger.log("Legacy cached prefs: $cachedPrefs failed to parse: $e.")
            }
        }
    }

    public override fun close() {
        synchronized(setupLock) {
            try {
                if (!isEnabled()) {
                    return
                }

                enabled = false

                config?.let { config ->
                    apiKeys.remove(config.apiKey)

                    config.integrations.forEach {
                        try {
                            it.uninstall()
                        } catch (e: Throwable) {
                            config.logger
                                .log("Integration ${it.javaClass.name} failed to uninstall: $e.")
                        }
                    }
                }

                queue?.stop()
                replayQueue?.stop()

                featureFlagsCalled.clear()

                endSession()
            } catch (e: Throwable) {
                config?.logger?.log("Close failed: $e.")
            }
        }
    }

    private var anonymousId: String
        get() {
            var anonymousId: String?
            synchronized(anonymousLock) {
                anonymousId = getPreferences().getValue(ANONYMOUS_ID) as? String
                if (anonymousId.isNullOrBlank()) {
                    var uuid = TimeBasedEpochGenerator.generate()
                    // when getAnonymousId method is available, pass-through the value for modification
                    config?.getAnonymousId?.let { uuid = it(uuid) }
                    anonymousId = uuid.toString()
                    this.anonymousId = anonymousId ?: ""
                }
            }
            return anonymousId ?: ""
        }
        set(value) {
            getPreferences().setValue(ANONYMOUS_ID, value)
        }

    private var distinctId: String
        get() {
            return getPreferences().getValue(
                DISTINCT_ID,
                defaultValue = anonymousId,
            ) as? String ?: ""
        }
        set(value) {
            getPreferences().setValue(DISTINCT_ID, value)
        }

    private var isIdentified: Boolean = false
        get() {
            synchronized(identifiedLock) {
                if (!isIdentifiedLoaded) {
                    isIdentified = getPreferences().getValue(IS_IDENTIFIED) as? Boolean
                        ?: (distinctId != anonymousId)
                    isIdentifiedLoaded = true
                }
            }
            return field
        }
        set(value) {
            synchronized(identifiedLock) {
                field = value
                getPreferences().setValue(IS_IDENTIFIED, value)
            }
        }

    private var isPersonProcessingEnabled: Boolean = false
        get() {
            synchronized(personProcessingLock) {
                if (!isPersonProcessingLoaded) {
                    isPersonProcessingEnabled =
                        getPreferences().getValue(PERSON_PROCESSING) as? Boolean
                            ?: false
                    isPersonProcessingLoaded = true
                }
            }
            return field
        }
        set(value) {
            synchronized(personProcessingLock) {
                // only set if its different to avoid IO since this is called more often
                if (field != value) {
                    field = value
                    getPreferences().setValue(PERSON_PROCESSING, value)
                }
            }
        }

    private fun buildProperties(
        distinctId: String,
        properties: Map<String, Any>?,
        userProperties: Map<String, Any>?,
        userPropertiesSetOnce: Map<String, Any>?,
        groups: Map<String, String>?,
        appendSharedProps: Boolean = true,
        appendGroups: Boolean = true,
    ): Map<String, Any> {
        val props = mutableMapOf<String, Any>()

        if (appendSharedProps) {
            val registeredPrefs = getPreferences().getAll()
            if (registeredPrefs.isNotEmpty()) {
                props.putAll(registeredPrefs)
            }

            config?.context?.getStaticContext()?.let {
                props.putAll(it)
            }

            config?.context?.getDynamicContext()?.let {
                props.putAll(it)
            }

            if (config?.sendFeatureFlagEvent == true) {
                featureFlags?.getFeatureFlags()?.let {
                    if (it.isNotEmpty()) {
                        val keys = mutableListOf<String>()
                        for (entry in it.entries) {
                            props["\$feature/${entry.key}"] = entry.value

                            // only add active feature flags
                            val active = entry.value as? Boolean ?: true

                            if (active) {
                                keys.add(entry.key)
                            }
                        }
                        props["\$active_feature_flags"] = keys
                    }
                }
            }

            userProperties?.let {
                props["\$set"] = it
            }

            userPropertiesSetOnce?.let {
                props["\$set_once"] = it
            }

            if (appendGroups) {
                // merge groups
                mergeGroups(groups)?.let {
                    props["\$groups"] = it
                }
            }

            props["\$is_identified"] = isIdentified

            props["\$process_person_profile"] = hasPersonProcessing()
        }

        // Session replay should have the SDK info as well
        config?.context?.getSdkInfo()?.let {
            props.putAll(it)
        }

        val isSessionReplayFlagActive = isSessionReplayFlagActive()

        PaylisherSessionManager.getActiveSessionId()?.let { sessionId ->
            val tempSessionId = sessionId.toString()
            props["\$session_id"] = tempSessionId
            // only Session replay needs $window_id
            if (!appendSharedProps && isSessionReplayFlagActive) {
                // Session replay requires $window_id, so we set as the same as $session_id.
                // the backend might fallback to $session_id if $window_id is not present next.
                props["\$window_id"] = tempSessionId
            }
        }

        properties?.let {
            props.putAll(it)
        }

        // only Session replay needs distinct_id also in the props
        // remove after https://github.com/Paylisher/paylisher/pull/18954 gets merged
        val propDistinctId = props["distinct_id"] as? String
        if (!appendSharedProps && isSessionReplayFlagActive && propDistinctId.isNullOrBlank()) {
            // distinctId is already validated hence not empty or blank
            props["distinct_id"] = distinctId
        }

        return props
    }

    private fun mergeGroups(givenGroups: Map<String, String>?): Map<String, String>? {
        val preferences = getPreferences()

        @Suppress("UNCHECKED_CAST")
        val groups = preferences.getValue(GROUPS) as? Map<String, String>
        val newGroups = mutableMapOf<String, String>()

        groups?.let {
            newGroups.putAll(it)
        }

        givenGroups?.let {
            newGroups.putAll(it)
        }

        return newGroups.ifEmpty { null }
    }

    public override fun capture(
        event: String,
        distinctId: String?,
        properties: Map<String, Any>?,
        userProperties: Map<String, Any>?,
        userPropertiesSetOnce: Map<String, Any>?,
        groups: Map<String, String>?,
    ) {
        try {
            if (!isEnabled()) {
                return
            }
            if (config?.optOut == true) {
                config?.logger?.log("Paylisher is in OptOut state.")
                return
            }

            val newDistinctId = distinctId ?: this.distinctId

            // if the user isn't identified but passed userProperties, userPropertiesSetOnce or groups,
            // we should still enable person processing since this is intentional
            if (userProperties?.isEmpty() == false || userPropertiesSetOnce?.isEmpty() == false || groups?.isEmpty() == false) {
                requirePersonProcessing("capture", ignoreMessage = true)
            }

            if (newDistinctId.isBlank()) {
                config?.logger?.log("capture call not allowed, distinctId is invalid: $newDistinctId.")
                return
            }

            var snapshotEvent = false
            if (event == "\$snapshot") {
                snapshotEvent = true
            }

            var groupIdentify = false
            if (event == GROUP_IDENTIFY) {
                groupIdentify = true
            }

            val mergedProperties =
                buildProperties(
                    newDistinctId,
                    properties = properties,
                    userProperties = userProperties,
                    userPropertiesSetOnce = userPropertiesSetOnce,
                    groups = groups,
                    // only append shared props if not a snapshot event
                    appendSharedProps = !snapshotEvent,
                    // only append groups if not a group identify event and not a snapshot
                    appendGroups = !groupIdentify,
                )

            // sanitize the properties or fallback to the original properties
            val sanitizedProperties =
                config?.propertiesSanitizer?.sanitize(mergedProperties.toMutableMap())
                    ?: mergedProperties

            val paylisherEvent =
                PaylisherEvent(
                    event,
                    newDistinctId,
                    properties = sanitizedProperties,
                )

            // Replay has its own queue
            if (snapshotEvent) {
                replayQueue?.add(paylisherEvent)
                return
            }

            queue?.add(paylisherEvent)
        } catch (e: Throwable) {
            config?.logger?.log("Capture failed: $e.")
        }
    }

    public override fun optIn() {
        if (!isEnabled()) {
            return
        }

        synchronized(optOutLock) {
            config?.optOut = false
            getPreferences().setValue(OPT_OUT, false)
        }
    }

    public override fun optOut() {
        if (!isEnabled()) {
            return
        }

        synchronized(optOutLock) {
            config?.optOut = true
            getPreferences().setValue(OPT_OUT, true)
        }
    }

    /**
     * Is Opt Out
     */
    public override fun isOptOut(): Boolean {
        if (!isEnabled()) {
            return true
        }
        return config?.optOut ?: true
    }

    public override fun screen(
        screenTitle: String,
        properties: Map<String, Any>?,
    ) {
        if (!isEnabled()) {
            return
        }

        val props = mutableMapOf<String, Any>()
        props["\$screen_name"] = screenTitle

        properties?.let {
            props.putAll(it)
        }

        capture("\$screen", properties = props)
    }

    public override fun alias(alias: String) {
        if (!isEnabled()) {
            return
        }

        if (!requirePersonProcessing("alias")) {
            return
        }

        val props = mutableMapOf<String, Any>()
        props["alias"] = alias

        capture("\$create_alias", properties = props)
    }

    public override fun identify(
        distinctId: String,
        userProperties: Map<String, Any>?,
        userPropertiesSetOnce: Map<String, Any>?,
    ) {
        if (!isEnabled()) {
            return
        }

        if (!requirePersonProcessing("identify")) {
            return
        }

        if (distinctId.isBlank()) {
            config?.logger?.log("identify call not allowed, distinctId is invalid: $distinctId.")
            return
        }

        val previousDistinctId = this.distinctId

        val props = mutableMapOf<String, Any>()
        val anonymousId = this.anonymousId
        if (anonymousId.isNotBlank()) {
            props["\$anon_distinct_id"] = anonymousId
        } else {
            config?.logger?.log("identify called with invalid anonymousId: $anonymousId.")
        }

        if (previousDistinctId != distinctId && !isIdentified) {
            // this has to be set before capture since this flag will be read during the event
            // capture
            synchronized(identifiedLock) {
                isIdentified = true
            }

            capture(
                "\$identify",
                distinctId = distinctId,
                properties = props,
                userProperties = userProperties,
                userPropertiesSetOnce = userPropertiesSetOnce,
            )

            // We keep the AnonymousId to be used by decide calls and identify to link the previousId
            if (previousDistinctId.isNotBlank()) {
                this.anonymousId = previousDistinctId
            } else {
                config?.logger?.log("identify called with invalid former distinctId: $previousDistinctId.")
            }
            this.distinctId = distinctId

            // only because of testing in isolation, this flag is always enabled
            if (reloadFeatureFlags) {
                reloadFeatureFlags()
            }
        } else {
            config?.logger?.log("already identified with id: $distinctId.")
        }
    }

    private fun hasPersonProcessing(): Boolean {
        return !(
                config?.personProfiles == PersonProfiles.NEVER ||
                        (
                                config?.personProfiles == PersonProfiles.IDENTIFIED_ONLY &&
                                        !isIdentified &&
                                        !isPersonProcessingEnabled
                                )
                )
    }

    private fun requirePersonProcessing(
        functionName: String,
        ignoreMessage: Boolean = false,
    ): Boolean {
        if (config?.personProfiles == PersonProfiles.NEVER) {
            if (!ignoreMessage) {
                config?.logger?.log("$functionName was called, but `personProfiles` is set to `never`. This call will be ignored.")
            }
            return false
        }
        isPersonProcessingEnabled = true
        return true
    }

    public override fun group(
        type: String,
        key: String,
        groupProperties: Map<String, Any>?,
    ) {
        if (!isEnabled()) {
            return
        }

        if (!requirePersonProcessing("group")) {
            return
        }

        val props = mutableMapOf<String, Any>()
        props["\$group_type"] = type
        props["\$group_key"] = key
        groupProperties?.let {
            props["\$group_set"] = it
        }

        val preferences = getPreferences()
        var reloadFeatureFlagsIfNewGroup = false

        synchronized(groupsLock) {
            @Suppress("UNCHECKED_CAST")
            val groups = preferences.getValue(GROUPS) as? Map<String, String>
            val newGroups = mutableMapOf<String, String>()

            groups?.let {
                val currentKey = it[type]

                if (key != currentKey) {
                    reloadFeatureFlagsIfNewGroup = true
                }

                newGroups.putAll(it)
            }
            newGroups[type] = key

            preferences.setValue(GROUPS, newGroups)
        }

        capture(GROUP_IDENTIFY, properties = props)

        // only because of testing in isolation, this flag is always enabled
        if (reloadFeatureFlags && reloadFeatureFlagsIfNewGroup) {
            loadFeatureFlagsRequest(null)
        }
    }

    public override fun reloadFeatureFlags(onFeatureFlags: PaylisherOnFeatureFlags?) {
        if (!isEnabled()) {
            return
        }
        loadFeatureFlagsRequest(onFeatureFlags)
    }

    private fun loadFeatureFlagsRequest(onFeatureFlags: PaylisherOnFeatureFlags?) {
        @Suppress("UNCHECKED_CAST")
        val groups = getPreferences().getValue(GROUPS) as? Map<String, String>

        val distinctId = this.distinctId
        val anonymousId = this.anonymousId

        if (distinctId.isBlank()) {
            config?.logger?.log("Feature flags not loaded, distinctId is invalid: $distinctId")
            return
        }

        featureFlags?.loadFeatureFlags(
            distinctId,
            anonymousId = anonymousId,
            groups,
            onFeatureFlags
        )
    }

    public override fun isFeatureEnabled(
        key: String,
        defaultValue: Boolean,
    ): Boolean {
        if (!isEnabled()) {
            return defaultValue
        }
        val value = featureFlags?.isFeatureEnabled(key, defaultValue) ?: defaultValue

        sendFeatureFlagCalled(key, value)

        return value
    }

    private fun sendFeatureFlagCalled(
        key: String,
        value: Any?,
    ) {
        var shouldSendFeatureFlagEvent = true
        synchronized(featureFlagsCalledLock) {
            val values = featureFlagsCalled[key] ?: mutableListOf()
            if (values.contains(value)) {
                shouldSendFeatureFlagEvent = false
            } else {
                values.add(value)
                featureFlagsCalled[key] = values
            }
        }

        if (config?.sendFeatureFlagEvent == true && shouldSendFeatureFlagEvent) {
            val props = mutableMapOf<String, Any>()
            props["\$feature_flag"] = key
            // value should never be nullabe anyway
            props["\$feature_flag_response"] = value ?: ""

            capture("\$feature_flag_called", properties = props)
        }
    }

    public override fun getFeatureFlag(
        key: String,
        defaultValue: Any?,
    ): Any? {
        if (!isEnabled()) {
            return defaultValue
        }
        val value = featureFlags?.getFeatureFlag(key, defaultValue) ?: defaultValue

        sendFeatureFlagCalled(key, value)

        return value
    }

    public override fun getFeatureFlagPayload(
        key: String,
        defaultValue: Any?,
    ): Any? {
        if (!isEnabled()) {
            return defaultValue
        }
        return featureFlags?.getFeatureFlagPayload(key, defaultValue) ?: defaultValue
    }

    public override fun flush() {
        if (!isEnabled()) {
            return
        }
        queue?.flush()
        replayQueue?.flush()
    }

    public override fun reset() {
        if (!isEnabled()) {
            return
        }

        // only remove properties, preserve BUILD and VERSION keys in order to to fix over-sending
        // of 'Application Installed' events and under-sending of 'Application Updated' events
        val except = listOf(VERSION, BUILD)
        getPreferences().clear(except = except)
        featureFlags?.clear()
        featureFlagsCalled.clear()
        synchronized(identifiedLock) {
            isIdentifiedLoaded = false
        }
        synchronized(personProcessingLock) {
            isPersonProcessingLoaded = false
        }

        endSession()
        startSession()

        // reload flags as anon user
        // only because of testing in isolation, this flag is always enabled
        if (reloadFeatureFlags) {
            reloadFeatureFlags()
        }
    }

    private fun isEnabled(): Boolean {
        if (!enabled) {
            config?.logger?.log("Setup isn't called.")
        }
        return enabled
    }

    public override fun register(
        key: String,
        value: Any,
    ) {
        if (!isEnabled()) {
            return
        }
        if (ALL_INTERNAL_KEYS.contains(key)) {
            config?.logger?.log("Key: $key is reserved for internal use.")
            return
        }
        getPreferences().setValue(key, value)
    }

    public override fun unregister(key: String) {
        if (!isEnabled()) {
            return
        }
        getPreferences().remove(key)
    }

    override fun distinctId(): String {
        if (!isEnabled()) {
            return ""
        }
        return distinctId
    }

    override fun debug(enable: Boolean) {
        if (!isEnabled()) {
            return
        }
        config?.debug = enable
    }

    override fun startSession() {
        if (!isEnabled()) {
            return
        }

        PaylisherSessionManager.startSession()
    }

    override fun endSession() {
        if (!isEnabled()) {
            return
        }

        PaylisherSessionManager.endSession()
    }

    override fun isSessionActive(): Boolean {
        if (!isEnabled()) {
            return false
        }

        return PaylisherSessionManager.isSessionActive()
    }

    // this is used in cases where we know the session is already active
    // so we spare another locker
    private fun isSessionReplayFlagActive(): Boolean {
        return config?.sessionReplay == true && featureFlags?.isSessionReplayFlagActive() == true
    }

    override fun isSessionReplayActive(): Boolean {
        // not checking isEnabled() here because isSessionActive already does that anyway
        return isSessionReplayFlagActive() && isSessionActive()
    }

    override fun getSessionId(): UUID? {
        if (!isEnabled()) {
            return null
        }

        return PaylisherSessionManager.getActiveSessionId()
    }

    override fun <T : PaylisherConfig> getConfig(): T? {
        @Suppress("UNCHECKED_CAST")
        return config as? T
    }

    public companion object : PaylisherInterface {
        private var shared: PaylisherInterface = Paylisher()
        private var defaultSharedInstance = shared

        private const val GROUP_IDENTIFY = "\$groupidentify"

        private val apiKeys = mutableSetOf<String>()

        private val signalService = SignalService()

        fun getSignal(): SignalService {
            return signalService;
        }

        @PaylisherVisibleForTesting
        public fun overrideSharedInstance(paylisher: PaylisherInterface) {
            shared = paylisher
        }

        @PaylisherVisibleForTesting
        public fun resetSharedInstance() {
            shared = defaultSharedInstance
        }

        /**
         * Setup the SDK and returns an instance that you can hold and pass it around
         * @param T the type of the Config
         * @property config the Config
         */
        public fun <T : PaylisherConfig> with(config: T): PaylisherInterface {
            val instance = Paylisher()
            instance.setup(config)
            return instance
        }

        @PaylisherVisibleForTesting
        internal fun <T : PaylisherConfig> withInternal(
            config: T,
            queueExecutor: ExecutorService,
            replayExecutor: ExecutorService,
            errorExecutor: ExecutorService,
            featureFlagsExecutor: ExecutorService,
            cachedEventsExecutor: ExecutorService,
            reloadFeatureFlags: Boolean,
        ): PaylisherInterface {
            val instance =
                Paylisher(
                    queueExecutor,
                    replayExecutor,
                    errorExecutor,
                    featureFlagsExecutor,
                    cachedEventsExecutor,
                    reloadFeatureFlags = reloadFeatureFlags,
                )
            instance.setup(config)
            return instance
        }

        public override fun <T : PaylisherConfig> setup(config: T) {
            shared.setup(config)
        }

        public override fun close() {
            shared.close()
        }

        public override fun capture(
            event: String,
            distinctId: String?,
            properties: Map<String, Any>?,
            userProperties: Map<String, Any>?,
            userPropertiesSetOnce: Map<String, Any>?,
            groups: Map<String, String>?,
        ) {
            if (event != "\$snapshot") {
                signalService.sendSignal(event)
            }

            shared.capture(
                event,
                distinctId = distinctId,
                properties = properties,
                userProperties = userProperties,
                userPropertiesSetOnce = userPropertiesSetOnce,
                groups = groups,
            )
        }

        public override fun identify(
            distinctId: String,
            userProperties: Map<String, Any>?,
            userPropertiesSetOnce: Map<String, Any>?,
        ) {
            shared.identify(
                distinctId,
                userProperties = userProperties,
                userPropertiesSetOnce = userPropertiesSetOnce,
            )
        }

        public override fun reloadFeatureFlags(onFeatureFlags: PaylisherOnFeatureFlags?) {
            shared.reloadFeatureFlags(onFeatureFlags)
        }

        public override fun isFeatureEnabled(
            key: String,
            defaultValue: Boolean,
        ): Boolean = shared.isFeatureEnabled(key, defaultValue = defaultValue)

        public override fun getFeatureFlag(
            key: String,
            defaultValue: Any?,
        ): Any? = shared.getFeatureFlag(key, defaultValue = defaultValue)

        public override fun getFeatureFlagPayload(
            key: String,
            defaultValue: Any?,
        ): Any? = shared.getFeatureFlagPayload(key, defaultValue = defaultValue)

        public override fun flush() {
            shared.flush()
        }

        public override fun reset() {
            shared.reset()
        }

        public override fun optIn() {
            shared.optIn()
        }

        public override fun optOut() {
            shared.optOut()
        }

        public override fun group(
            type: String,
            key: String,
            groupProperties: Map<String, Any>?,
        ) {
            shared.group(type, key, groupProperties = groupProperties)
        }

        public override fun screen(
            screenTitle: String,
            properties: Map<String, Any>?,
        ) {
            shared.screen(screenTitle, properties = properties)
        }

        public override fun alias(alias: String) {
            shared.alias(alias)
        }

        public override fun isOptOut(): Boolean = shared.isOptOut()

        public override fun register(
            key: String,
            value: Any,
        ) {
            shared.register(key, value)
        }

        public override fun unregister(key: String) {
            shared.unregister(key)
        }

        override fun distinctId(): String = shared.distinctId()

        override fun debug(enable: Boolean) {
            shared.debug(enable)
        }

        override fun startSession() {
            shared.startSession()
        }

        override fun endSession() {
            shared.endSession()
        }

        override fun isSessionActive(): Boolean {
            return shared.isSessionActive()
        }

        override fun isSessionReplayActive(): Boolean {
            return shared.isSessionReplayActive()
        }

        override fun getSessionId(): UUID? {
            return shared.getSessionId()
        }

        override fun <T : PaylisherConfig> getConfig(): T? {
            return shared.getConfig()
        }
    }
}