package com.paylisher.internal.replay

import com.paylisher.PaylisherInternal

@PaylisherInternal
public class RRMutatedNode(
    public val wireframe: RRWireframe,
    public val parentId: Int? = null,
)
