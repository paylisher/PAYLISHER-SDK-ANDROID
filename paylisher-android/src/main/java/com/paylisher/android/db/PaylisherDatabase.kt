package com.paylisher.android.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [NotificationEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class PaylisherDatabase : RoomDatabase() {
    abstract fun NotificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: PaylisherDatabase? = null

        fun getInstance(context: Context): PaylisherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PaylisherDatabase::class.java,
                    "paylisher-database"
                )
                    // Uncomment the line below only during development to reset the database
//                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
