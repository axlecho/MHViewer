package com.hippo.ehviewer

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.axlecho.api.MHApi
import com.axlecho.api.MHApiSource
import com.axlecho.api.MHComicInfo
import com.hippo.ehviewer.client.data.GalleryInfo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class ImportService : Service() {

    companion object {
        private val TAG = "import"
        val ACTION_START = "start"
        val KEY_SOURCE = "source"
        val KEY_TARGET = "target"
    }

    private var mNotifyManager: NotificationManager? = null
    private var result = arrayListOf<MHComicInfo>()
    private var success = 0
    private var failed = 0
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mNotifyManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        handleIntent(intent)
        return Service.START_STICKY
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) {
            return
        }

        val action = intent.action
        if (ACTION_START == action) {
            val source = intent.getParcelableExtra<MHApiSource>(KEY_SOURCE)
            val target = intent.getParcelableExtra<MHApiSource>(KEY_TARGET)
            startImport(source, target)
        }
    }

    private fun startImport(source: MHApiSource, target: MHApiSource) {
        val uid = "axlecho"
        MHApi.INSTANCE.select(source)
        val handle = MHApi.INSTANCE.getAllCollection(uid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { t -> Log.d(TAG, t.message) }
                .doOnComplete { switch(target) }
                .subscribe {
                    sendNotification("===> " + it.currentPage + " page", "Getting collections")
                    result.addAll(it.datas)
                }

    }

    private fun switch(target: MHApiSource) {
        MHApi.INSTANCE.select(target)
        if (result.isEmpty()) {
            return
        }

        val error = MHComicInfo(-1, "", "", "", -1, "", "", 0.0f, false, MHApiSource.Hanhan);

        val handle = Observable.just(result)
                .flatMapIterable { ids -> ids }
                .flatMap {
                    MHApi.INSTANCE.switchSource(it, target)
                            .onErrorResumeNext(Observable.just(error))
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { t -> Log.d(TAG, t.message) }
                .doOnComplete { sendNotification("success $success,failed $failed", "Done") }
                .subscribe {
                    if (it.gid != -1L) {
                        sendNotification("===> " + it.title, "Find item on " + target.name)
                        save(it)
                        success ++
                    } else {
                        failed ++
                    }
                }
    }

    private fun save(info: MHComicInfo) {
        EhDB.putLocalFavorites(GalleryInfo(info))
    }

    private fun sendNotification(message: String, title: String) {
        val builder = NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(message)
                .setContentTitle(title)
        mNotifyManager?.notify(1, builder.build())
    }
}
