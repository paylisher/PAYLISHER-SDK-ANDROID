package com.paylisher.android.replay.internal

import com.paylisher.internal.replay.RRWireframe

internal class ViewTreeSnapshotStatus(
    val listener: NextDrawListener,
    var sentFullSnapshot: Boolean = false,
    var sentMetaEvent: Boolean = false,
    var keyboardVisible: Boolean = false,
    var lastSnapshot: RRWireframe? = null,
)
