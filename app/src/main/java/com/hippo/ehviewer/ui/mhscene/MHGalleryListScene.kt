package com.hippo.ehviewer.ui.mhscene

import android.os.Bundle
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.ui.scene.BaseScene
import com.hippo.ehviewer.ui.scene.GalleryListScene
import com.hippo.scene.Announcer

class MHGalleryListScene : BaseScene() {

    companion object {
        val KEY_ACTION = "action"
        val ACTION_LIST_URL_BUILDER = "action_list_url_builder"
        val KEY_LIST_URL_BUILDER = "list_url_builder"

        fun getStartAnnouncer(lub: ListUrlBuilder): Announcer {
            val args = Bundle()
            args.putString(KEY_ACTION, ACTION_LIST_URL_BUILDER)
            args.putParcelable(KEY_LIST_URL_BUILDER, lub)
            return Announcer(GalleryListScene::class.java).setArgs(args)
        }
    }
}