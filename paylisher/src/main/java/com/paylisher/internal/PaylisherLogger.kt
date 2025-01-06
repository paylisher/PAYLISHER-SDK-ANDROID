package com.paylisher.internal

import com.paylisher.PaylisherInternal

/**
 * Interface for logging debug messages into the System out or Logcat depending on the implementation
 */
@PaylisherInternal
public interface PaylisherLogger {
    public fun log(message: String)

    public fun isEnabled(): Boolean
}
