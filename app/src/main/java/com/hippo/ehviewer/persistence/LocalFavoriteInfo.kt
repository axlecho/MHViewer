package com.hippo.ehviewer.persistence

import androidx.room.*
import com.hippo.ehviewer.client.data.GalleryInfo

@Entity(tableName = "LOCAL_FAVORITES")
data class LocalFavoriteInfo(
        @ColumnInfo(name = "GID") val gid: String,
        @ColumnInfo(name = "TOKEN") var token: String? = null,
        @ColumnInfo(name = "TITLE") var title: String? = null,
        @ColumnInfo(name = "TITLE_JPN") var titleJpn: String? = null,
        @ColumnInfo(name = "THUMB") var thumb: String? = null,
        @ColumnInfo(name = "CATEGORY") var category: Int,
        @ColumnInfo(name = "POSTED") var posted: String? = null,
        @ColumnInfo(name = "UPLOADER") var uploader: String? = null,
        @ColumnInfo(name = "RATING") var rating: Float = 0.0f,
        @ColumnInfo(name = "SIMPLE_LANGUAGE") var simpleLanguage: String? = null,

        @ColumnInfo(name = "TIME") var time: Long = 0,
        @PrimaryKey @ColumnInfo(name = "ID") var id: String) {

    constructor(info: GalleryInfo) : this(info.gid, info.token, info.title, info.titleJpn,
            info.thumb, info.category, info.posted, info.uploader, info.rating, info.simpleLanguage,
            System.currentTimeMillis(), info.id)
}

@Dao
interface MHLocalFavoriteInfoDao {
    @Query("SELECT * FROM LOCAL_FAVORITES ORDER BY TIME DESC ")
    fun list(): List<LocalFavoriteInfo>

    @Query("SELECT * FROM LOCAL_FAVORITES WHERE ID LIKE :source ORDER BY TIME DESC ")
    fun list(source: String): List<LocalFavoriteInfo>

    @Query("SELECT * FROM LOCAL_FAVORITES WHERE TITLE LIKE :keyword ORDER BY TIME DESC ")
    fun search(keyword: String): List<LocalFavoriteInfo>

    @Query("SELECT * FROM LOCAL_FAVORITES WHERE id = :id")
    fun load(id: String): LocalFavoriteInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(info: LocalFavoriteInfo)

    @Query("DELETE FROM LOCAL_FAVORITES WHERE id = :id")
    fun delete(id: String)

}

