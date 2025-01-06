package com.paylisher.internal

import com.paylisher.PaylisherInternal

/**
 * NoOp Logger
 */
@PaylisherInternal
public class PaylisherNoOpLogger : PaylisherLogger {
    override fun log(message: String) {
    }

    override fun isEnabled(): Boolean = false
}
