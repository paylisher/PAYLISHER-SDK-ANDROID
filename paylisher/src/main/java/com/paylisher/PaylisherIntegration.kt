package com.paylisher

/**
 * Integration interface for capturing events automatically or adding plugins to the Paylisher SDK
 */
public interface PaylisherIntegration {
    /**
     * Install the Integration after the SDK is setup
     */
    public fun install()

    /**
     * Uninstall the Integration after the SDK is closed
     */
    public fun uninstall() {
    }
}
