package com.paylisher.internal.replay

import com.paylisher.PaylisherInternal

@PaylisherInternal
public class RRIncrementalSnapshotEvent(
    mutationData: RRIncrementalMutationData? = null,
    timestamp: Long,
) : RREvent(
        type = RREventType.IncrementalSnapshot,
        data = mutationData,
        timestamp = timestamp,
    )
