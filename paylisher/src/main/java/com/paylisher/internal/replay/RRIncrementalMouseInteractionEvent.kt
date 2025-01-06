package com.paylisher.internal.replay

import com.paylisher.PaylisherInternal

@PaylisherInternal
public class RRIncrementalMouseInteractionEvent(
    mouseInteractionData: RRIncrementalMouseInteractionData? = null,
    timestamp: Long,
) : RREvent(
        type = RREventType.IncrementalSnapshot,
        data = mouseInteractionData,
        timestamp = timestamp,
    )
