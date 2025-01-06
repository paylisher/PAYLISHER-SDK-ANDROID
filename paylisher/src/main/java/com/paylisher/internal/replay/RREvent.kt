package com.paylisher.internal.replay

import com.paylisher.PaylisherInternal
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

@IgnoreJRERequirement
@PaylisherInternal
public open class RREvent(
    public val type: RREventType,
    public val timestamp: Long,
    public val data: Any? = null,
)
