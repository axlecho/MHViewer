package com.hippo.ehviewer.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "RECORDS")
data class ReadingRecord(
        @PrimaryKey @ColumnInfo(name = "ID") val id: String,
        @ColumnInfo(name = "UPDATE_TIME") var update_time: Long,
        @ColumnInfo(name = "READ_TIME") var read_time: Long? = null,
        @ColumnInfo(name = "CHAPTER_INFO") var chapter_info: String? = null
)