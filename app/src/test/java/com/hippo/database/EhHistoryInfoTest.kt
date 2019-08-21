package com.hippo.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hippo.ehviewer.client.database.EhDatabase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EhHistoryInfoTest {
    private lateinit var database: EhDatabase
    @Before
    fun initDB() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), EhDatabase::class.java)
                .build()


    }

    @After
    fun closeDB() {
        database.close()
    }

    @Test
    fun base() {

    }
}