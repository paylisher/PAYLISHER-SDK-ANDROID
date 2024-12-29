package com.paylisher.android.db

import androidx.room.TypeConverter
import java.util.Date

// Date Converter for Room
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}