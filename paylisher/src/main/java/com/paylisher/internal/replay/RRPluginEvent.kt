package com.paylisher.internal.replay

import com.paylisher.PaylisherInternal

@PaylisherInternal
public class RRPluginEvent(plugin: String, payload: Map<String, Any>, timestamp: Long) : RREvent(
    type = RREventType.Plugin,
    data =
        mapOf(
            "plugin" to plugin,
            "payload" to payload,
        ),
    timestamp = timestamp,
)
