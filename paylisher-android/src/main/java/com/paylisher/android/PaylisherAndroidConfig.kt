package com.paylisher.android

import com.paylisher.PaylisherConfig
import com.paylisher.PaylisherExperimental
import com.paylisher.android.replay.PaylisherSessionReplayConfig

/**
 * The SDK Config
 * @property apiKey the Paylisher API Key
 * @property captureApplicationLifecycleEvents captures lifecycle events such as app installed, app updated, app opened and backgrounded
 * @property captureDeepLinks captures deep links events
 * @property captureScreenViews captures screen views events
 */
public open class PaylisherAndroidConfig
    @JvmOverloads
    constructor(
        apiKey: String,
        host: String = DEFAULT_HOST,
        public var captureApplicationLifecycleEvents: Boolean = true,
        public var captureDeepLinks: Boolean = true,
        public var captureScreenViews: Boolean = true,
        @PaylisherExperimental
        public var sessionReplayConfig: PaylisherSessionReplayConfig = PaylisherSessionReplayConfig(),
    ) : PaylisherConfig(apiKey, host)
