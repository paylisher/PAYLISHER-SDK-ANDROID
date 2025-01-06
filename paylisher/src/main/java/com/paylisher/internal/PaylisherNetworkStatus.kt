package com.paylisher.internal

import com.paylisher.PaylisherInternal

/**
 * Interface for checking the network connectivity before trying to send events over the wire
 */
@PaylisherInternal
public fun interface PaylisherNetworkStatus {
    public fun isConnected(): Boolean
}
