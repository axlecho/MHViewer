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
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class ImportService : Service() {

    companion object {
        private val TAG = "import"
        val ACTION_START = "start"
        val KEY_SOURCE = "source"
        val KEY_TARGET = "target"
        val KEY_LOCAL = "local"
    }

    private var mNotifyManager: NotificationManager? = null
    private var result = arrayListOf<MHComicInfo>()
    private var success = 0
    private var failed = 0
    private var collectionHandle: Disposable = object : Disposable {
        override fun dispose() {}

        override fun isDisposed(): Boolean {
            return true
        }
    }
    private var importHandle: Disposable = object : Disposable {
        override fun dispose() {

        }

        override fun isDisposed(): Boolean {
            return true
        }
    }

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

        if (!collectionHandle.isDisposed || !importHandle.isDisposed) {
            return
        }

        val action = intent.action
        if (ACTION_START == action) {
            val source = intent.getStringExtra(KEY_SOURCE)
            val target = intent.getStringExtra(KEY_TARGET)
            val isLocal = intent.getBooleanExtra(KEY_LOCAL, false)
            result.clear()
            if (isLocal) {
                startImportLocal(source, target)
            } else {
                startImport(source, target)
            }
        }
    }

    private fun startImport(source: String, target: String) {
        val uid = "axlecho"
        MHApi.INSTANCE.get(source)
        collectionHandle = MHApi.INSTANCE.getAllCollection(uid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { t -> Log.d(TAG, t.message) }
                .doOnComplete { switch(target) }
                .subscribe {
                    sendNotification("===> " + it.currentPage, "Getting collections", it.pages, it.currentPage)
                    result.addAll(it.datas)
                }
    }

    private fun startImportLocal(source: String, target: String) {
        val ret = EhDB.getLocalFavorites(source)
        for (r in ret) {
            result.add(MHComicInfo(r.gid, r.title, "", "", -1, "", "", 0.0f, false, source))
        }
        switch(target)
    }

    private fun switch(target: String) {
        MHApi.INSTANCE.get(target)
        if (result.isEmpty()) {
            return
        }

        var current = 0
        val error = MHComicInfo("-1", "", "", "", -1, "", "", 0.0f, false, MHApiSource.UnKnown)

        importHandle = Observable.fromIterable(result)
                .concatMap { item ->
                    Observable.interval((3000..5000).random().toLong(), TimeUnit.MILLISECONDS)
                            .take(1)
                            .map { item }
                            .flatMap {
                                MHApi.INSTANCE.switchSource(it, target)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .doOnError {
                                            sendNotification("failed", item.title, result.size, current)
                                        }
                                        .onErrorResumeNext(Observable.just(error))
                            }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { e -> Log.d(TAG, e.message) }
                .doOnComplete { sendNotification("success $success,failed $failed", "Done") }
                .subscribe {
                    if (it.gid != "-1") {
                        save(it)
                        success++
                        sendNotification("success", it.title, result.size, current)
                    } else {
                        failed++

                    }
                    current++
                }
    }

    private fun save(info: MHComicInfo) {
        EhDB.putLocalFavorites(GalleryInfo(info))
    }

    private fun sendNotification(message: String, title: String, total: Int = 100, current: Int = 100) {
        val builder = NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(message)
                .setContentTitle(title)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(false)
                .setProgress(total, current, false)

        mNotifyManager?.notify(1, builder.build())
    }
}
