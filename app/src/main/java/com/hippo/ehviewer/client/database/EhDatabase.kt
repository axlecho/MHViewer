package com.hippo.ehviewer.client.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hippo.ehviewer.client.module.EhHistoryInfo

@Database(entities = [EhHistoryInfo::class],version  = 6)
abstract class EhDatabase : RoomDatabase() {
    abstract fun getHistoryInfoDao() : EhHistoryInfoDao
}