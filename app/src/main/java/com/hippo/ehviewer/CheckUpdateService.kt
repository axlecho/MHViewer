package com.hippo.ehviewer

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.axlecho.api.MHApi
import com.axlecho.api.MHComicDetail
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.dao.ReadingRecord
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class CheckUpdateService : Service() {

    companion object {
        val ACTION_START = "start"
    }

    private var mListener: UpdateListener? = null
    private val mBinder = CheckUpdateBinder()

    interface UpdateListener {
        fun update(done: Boolean, current: Int, total: Int)
    }

    inner class CheckUpdateBinder : Binder() {
        fun setListener(listener: UpdateListener) {
            mListener = listener
        }
    }

    private var mNotifyManager: NotificationManager? = null
    private var handle: Disposable = object : Disposable {
        override fun isDisposed(): Boolean {
            return true
        }

        override fun dispose() {}

    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
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

        if (!handle.isDisposed) {
            return
        }

        val action = intent.action
        if (CheckUpdateService.ACTION_START == action) {
            checkUpdate()
        }
    }

    private fun checkUpdate() {
        var current = 1
        val infos = EhDB.getAllLocalFavorites()
        handle = Flowable.just(infos)
                .flatMapIterable { list -> list }
                .parallel(4)
                .concatMap {
                    MHApi.INSTANCE.select(it.source).info(it.gid)
                            .onErrorResumeNext(Observable.empty())
                            .toFlowable(BackpressureStrategy.BUFFER)
                            .subscribeOn(Schedulers.io())
                }

                .runOn(Schedulers.io())
                .sequential()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    sendNotification(it.info.posted, "Check ${it.info.title}", infos.size, current)
                    current++
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

    private fun sendNotification(message: String, title: String, total: Int = 100, current: Int = 100) {
        val builder = NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(message)
                .setContentTitle(title)
                .setProgress(total, current, false)
        mNotifyManager?.notify(1, builder.build())
        mListener?.update(total == current, current, total)
    }
}