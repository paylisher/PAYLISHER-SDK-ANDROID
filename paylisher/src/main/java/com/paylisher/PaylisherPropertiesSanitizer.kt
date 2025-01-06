package com.paylisher

/**
 * Hook to sanitize the event properties
 */
public fun interface PaylisherPropertiesSanitizer {
    /**
     * Sanitizes the event properties
     * @param properties the event properties to sanitize
     * @return the sanitized properties
     */
    public fun sanitize(properties: MutableMap<String, Any>): Map<String, Any>
}
