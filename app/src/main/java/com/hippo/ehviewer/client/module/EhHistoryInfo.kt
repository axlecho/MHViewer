package com.hippo.ehviewer.client.module

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity
data class EhHistoryInfo(
        var mode: Int = 0,
        var time: Long = 0,
        @PrimaryKey val id:String
) : Parcelable