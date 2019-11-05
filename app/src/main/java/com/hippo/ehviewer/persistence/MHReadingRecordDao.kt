package com.hippo.ehviewer.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Completable

@Dao
interface MHReadingRecordDao {

    @Query("SELECT * FROM RECORDS")
    fun list(): List<ReadingRecord>

    @Query("SELECT * FROM RECORDS WHERE id = :id")
    fun load(id: String): ReadingRecord

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(record: ReadingRecord): Completable

}