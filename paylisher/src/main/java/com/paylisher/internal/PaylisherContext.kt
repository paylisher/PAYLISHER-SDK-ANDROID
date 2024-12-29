package com.paylisher.internal

import com.paylisher.PaylisherInternal

/**
 * An Interface that reads the static and dynamic context
 * For example, screen's metrics, app's name and version, device details, connectivity status
 */
@PaylisherInternal
public interface PaylisherContext {
    public fun getStaticContext(): Map<String, Any>

    public fun getDynamicContext(): Map<String, Any>

    public fun getSdkInfo(): Map<String, Any>
}
