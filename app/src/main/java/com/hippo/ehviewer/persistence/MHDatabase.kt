package com.hippo.ehviewer.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = arrayOf(ReadingRecord::class), version = 5)
abstract class MHDatabase : RoomDatabase() {

    abstract fun readingRecord(): MHReadingRecordDao

    companion object {

        @Volatile
        private var INSTANCE: MHDatabase? = null

        fun getInstance(context: Context): MHDatabase =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
                }

        private fun buildDatabase(context: Context) = Room
                .databaseBuilder(context.applicationContext, MHDatabase::class.java, "eh.db")
                .allowMainThreadQueries()
                .build()
    }
}