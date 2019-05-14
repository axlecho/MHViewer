package com.hippo.ehviewer

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.axlecho.api.MHApi
import com.axlecho.api.MHComicDetail
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.dao.ReadingRecord
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.flatMapIterable
import io.reactivex.schedulers.Schedulers

class CheckUpdateService : Service() {

    companion object {
        val ACTION_START = "start"
    }

    private var mNotifyManager: NotificationManager? = null

    override fun onBind(intent: Intent?): IBinder? {
        TODO("not implemented")
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
        if (CheckUpdateService.ACTION_START == action) {
            checkUpdate()
        }
    }

    private fun checkUpdate() {
        val infos = EhDB.getAllLocalFavorites()
        val handle = Observable.just(infos)
                .flatMapIterable()
                .concatMap {
                    MHApi.INSTANCE.select(it.source).info(it.gid)
                            .onErrorResumeNext(Observable.empty())
                }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    sendNotification("", "Check ${it.info.title}")
                    update(it)
                }
    }

    private fun update(item: MHComicDetail) {
        val detail = GalleryDetail(item)
        var record = EhDB.getReadingRecord(detail.id)
        if (record == null) {
            record = ReadingRecord()
        }
        record.id = detail.id
        record.update_time = detail.updateTime

        EhDB.putReadingRecord(record)
    }

    private fun sendNotification(message: String, title: String) {
        val builder = NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(message)
                .setContentTitle(title)
        mNotifyManager?.notify(1, builder.build())
    }
}