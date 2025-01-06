package com.paylisher.internal

import com.paylisher.PaylisherConfig
import com.paylisher.PaylisherInternal

/**
 * Logs the messages using System.out only if config.debug is enabled
 * @property config the Config
 */
@PaylisherInternal
public class PaylisherPrintLogger(private val config: PaylisherConfig) : PaylisherLogger {
    override fun log(message: String) {
        // isEnabled can be abstracted in another class (refactor needed).
        if (isEnabled()) {
            println(message)
        }
    }

    override fun isEnabled(): Boolean {
        return config.debug
    }
}
