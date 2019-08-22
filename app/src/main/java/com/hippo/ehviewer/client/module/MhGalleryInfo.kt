package com.hippo.ehviewer.client.module

import android.os.Parcelable
import com.axlecho.api.MHComicInfo
import com.hippo.ehviewer.dao.HistoryInfo
import com.hippo.ehviewer.dao.LocalFavoriteInfo
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MhGalleryInfo(
        var gid: String? = null,
        var token: String? = null,
        var title: String? = null,
        var titleJpn: String? = null,
        var thumb: String? = null,
        var category: Int = 0,
        var posted: String? = null,
        var uploader: String? = null,
        var rating: Float = 0.0f,
        var rated: Boolean = false,
        var simpleTags: Array<String>? = null,
        var pages: Int = 0,
        var thumbWidth: Int = 0,
        var thumbHeight: Int = 0,
        var spanSize: Int = 0,
        var spanIndex: Int = 0,
        var spanGroupIndex: Int = 0,
        var source: String? = null,
        var simpleLanguage: String? = null,
        var favoriteSlot: Int = -2,
        var favoriteName: String? = null,
        var chapterId: String? = null
) : Parcelable {

    val id: String get() = "$gid@$source"
    fun setCid(_cid: String) {
        chapterId = _cid
    }

    fun getCid(): String {
        return "$gid-$chapterId@$source"
    }

    constructor(info: MHComicInfo) : this() {
        this.gid = info.gid
        this.thumb = info.thumb
        this.category = info.category
        this.title = info.title
        this.pages = 0
        this.posted = info.posted
        this.rated = info.rated
        this.rating = info.rating
        this.titleJpn = info.titleJpn
        this.uploader = info.uploader
        this.source = info.source
    }

    constructor(info: LocalFavoriteInfo) : this() {
        this.source = info.id.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        this.title = info.title
        this.rating = info.rating
        this.category = info.category
        this.uploader = info.uploader
        this.titleJpn = info.titleJpn
        this.rated = info.rated
        this.posted = info.posted
        this.pages = info.pages
        this.thumb = info.thumb
        this.setCid(info.cid)
        this.gid = info.gid
        this.favoriteName = info.favoriteName
        this.favoriteSlot = info.favoriteSlot
        this.simpleLanguage = info.simpleLanguage
        this.simpleTags = info.simpleTags
        this.spanGroupIndex = info.spanGroupIndex
        this.spanIndex = info.spanIndex
        this.spanSize = info.spanSize
        this.thumbHeight = info.thumbHeight
        this.thumbWidth = info.thumbWidth
        this.token = info.token
    }

    constructor(info: HistoryInfo) : this() {
        this.source = info.id.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        this.title = info.title
        this.rating = info.rating
        this.category = info.category
        this.uploader = info.uploader
        this.titleJpn = info.titleJpn
        this.rated = info.rated
        this.posted = info.posted
        this.pages = info.pages
        this.thumb = info.thumb
        this.setCid(info.cid)
        this.gid = info.gid
        this.favoriteName = info.favoriteName
        this.favoriteSlot = info.favoriteSlot
        this.simpleLanguage = info.simpleLanguage
        this.simpleTags = info.simpleTags
        this.spanGroupIndex = info.spanGroupIndex
        this.spanIndex = info.spanIndex
        this.spanSize = info.spanSize
        this.thumbHeight = info.thumbHeight
        this.thumbWidth = info.thumbWidth
        this.token = info.token
    }
}
