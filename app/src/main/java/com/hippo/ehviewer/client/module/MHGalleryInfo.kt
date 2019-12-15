package com.hippo.ehviewer.client.module

data class MHGalleryInfo(
        val gid: String,
        val source: String,
        val title: String,
        val thumb: String,
        val posted: String,
        val uploader: String,
        val rating: Float = 0.0f,
        val rated: Boolean = false,
        val category: Int = 0,
        val pages: Int = 0,
        val id: String = "$gid@$source"
)