package com.paylisher.internal.replay

import com.paylisher.PaylisherInternal

@PaylisherInternal
public class RRLoadedEvent(timestamp: Long) : RREvent(
    type = RREventType.Load,
    timestamp = timestamp,
)
