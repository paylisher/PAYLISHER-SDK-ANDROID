package com.paylisher.internal.replay

import com.paylisher.PaylisherInternal

@PaylisherInternal
public class RRDomContentLoadedEvent(timestamp: Long) : RREvent(
    type = RREventType.DomContentLoaded,
    timestamp = timestamp,
)
