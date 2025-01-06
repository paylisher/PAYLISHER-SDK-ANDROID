package com.paylisher.internal.replay

import com.paylisher.PaylisherInternal

@PaylisherInternal
public class RRIncrementalMutationData(
    public val adds: List<RRMutatedNode>? = null,
    public val removes: List<RRRemovedNode>? = null,
    // updates and adds share the same format
    public val updates: List<RRMutatedNode>? = null,
    public val source: RRIncrementalSource = RRIncrementalSource.Mutation,
)
