package com.paylisher.internal.replay

import com.paylisher.PaylisherInternal

@PaylisherInternal
public class RRMetaEvent(width: Int, height: Int, timestamp: Long, href: String) : RREvent(
    type = RREventType.Meta,
    data =
        mapOf(
            "href" to href,
            "width" to width,
            "height" to height,
        ),
    timestamp = timestamp,
)
