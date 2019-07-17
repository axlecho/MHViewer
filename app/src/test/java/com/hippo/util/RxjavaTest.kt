package com.hippo.util

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import java.util.concurrent.TimeUnit

class RxjavaTest {
    @Test
    fun timerTest() {
        val now = System.currentTimeMillis()
        val list = mutableListOf(1, 2, 3, 4, 5, 6, 7, 8, 9)

        //  val timer = Observable.interval((1000..3000).random().toLong(), TimeUnit.MILLISECONDS)
        val worker = Observable.fromIterable(list)
                .concatMap { item -> Observable.interval(1, TimeUnit.SECONDS)
                            .take(1)
                            .map { item }
                            .flatMap { a ->
                                Observable.create<Int> {
                                    println("doing $a @" + (System.currentTimeMillis() - now))
                                    it.onNext(a)
                                    it.onComplete()
                                }
                            }
                            .doOnSubscribe {
                                println("subscribe in @" + (System.currentTimeMillis() - now))
                            }
                }
                .subscribeOn(Schedulers.computation())

        worker
                .subscribe {
                    println("done $it @" + (System.currentTimeMillis() - now))
                }
        Thread.sleep(20000)

    }
}