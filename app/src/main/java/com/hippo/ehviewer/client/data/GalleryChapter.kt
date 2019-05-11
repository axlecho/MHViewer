package com.hippo.ehviewer.client.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GalleryChapter(val title: String, val url: String, var read: Boolean) : Parcelable