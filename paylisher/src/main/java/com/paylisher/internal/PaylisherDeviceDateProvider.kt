package com.paylisher.internal

import java.util.Calendar
import java.util.Date

/**
 * The implementation of the PaylisherDateProvider interface that reads the current time
 *  and more based on the device's clock.
 */
internal class PaylisherDeviceDateProvider : PaylisherDateProvider {
    override fun currentDate(): Date {
        val cal = Calendar.getInstance()
        return cal.time
    }

    override fun addSecondsToCurrentDate(seconds: Int): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.SECOND, seconds)
        return cal.time
    }

    override fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    override fun nanoTime(): Long {
        return System.nanoTime()
    }
}
