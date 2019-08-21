package com.hippo.ehviewer.client.database

import androidx.room.Dao
import androidx.room.Insert
import com.hippo.ehviewer.client.module.EhHistoryInfo

@Dao
interface EhHistoryInfoDao {
    @Insert
    fun insert(history:EhHistoryInfo)
}