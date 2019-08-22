package com.hippo.ehviewer.client.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.module.EhHistoryInfo

@Database(entities = [EhHistoryInfo::class], version = 6)
abstract class EhDatabase : RoomDatabase() {
    abstract fun getHistoryInfoDao(): EhHistoryInfoDao
}

class EhDB {
    companion object {
        fun getAllLocalFavorites(): List<GalleryInfo> {
            return mutableListOf()
        }

        fun getLocalFavorites(source: String): List<GalleryInfo> {
            return mutableListOf()
        }

        fun putLocalFavorites(info: GalleryInfo) {

        }

        fun putLocalFavorites(list: List<GalleryInfo>) {

        }

        fun containLocalFavorites(info: GalleryInfo): Boolean {
            return false
        }

        fun removeLocalFavorites(info: GalleryInfo) {

        }

        fun removeLocalFavorites(info: List<GalleryInfo>) {

        }

        fun searchLocalFavorites(info: String): List<GalleryInfo> {
            return mutableListOf()
        }
    }
}