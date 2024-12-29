package com.paylisher.internal

import com.paylisher.PaylisherInternal
import java.util.concurrent.ThreadFactory

/**
 * A Thread factory for Executors
 * @property threadName the threadName
 */
@PaylisherInternal
public class PaylisherThreadFactory(private val threadName: String) : ThreadFactory {
    override fun newThread(runnable: Runnable): Thread {
        return Thread(runnable).apply {
            isDaemon = true
            name = threadName
        }
    }
}
