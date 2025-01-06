package com.paylisher.internal.replay

import com.paylisher.Paylisher
import com.paylisher.PaylisherInternal

@PaylisherInternal
public fun List<RREvent>.capture() {
    val properties =
        mutableMapOf(
            "\$snapshot_data" to this,
            "\$snapshot_source" to "mobile",
        )
    Paylisher.capture("\$snapshot", properties = properties)
}
