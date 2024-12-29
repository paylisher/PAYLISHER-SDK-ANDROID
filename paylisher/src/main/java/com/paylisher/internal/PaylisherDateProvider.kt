package com.paylisher.internal

import com.paylisher.PaylisherInternal
import java.util.Date

/**
 * Interface to read the current Date
 */
@PaylisherInternal
public interface PaylisherDateProvider {
    public fun currentDate(): Date

    public fun addSecondsToCurrentDate(seconds: Int): Date

    public fun currentTimeMillis(): Long

    public fun nanoTime(): Long
}
