package com.hippo.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.database.EhDB
import com.hippo.ehviewer.client.database.EhDatabase
import org.junit.After
import org.junit.Assert
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

    @Test
    fun testFavorites() {
        val source = "Manhuagui"
        val keyword = "good"

        val a = GalleryInfo()
        a.gid = "1"

        val list = mutableListOf<GalleryInfo>()
        for (i in 1..10) {
            val info = GalleryInfo()
            info.gid = "$i"
            list.add(info)
        }

        // com.hippo.ehviewer.EhDB.getAllLocalFavorites()
        // com.hippo.ehviewer.EhDB.getLocalFavorites(source)
        // com.hippo.ehviewer.EhDB.putLocalFavorites(a)
        // com.hippo.ehviewer.EhDB.putLocalFavorites(list)
        // com.hippo.ehviewer.EhDB.containLocalFavorites(a)
        // com.hippo.ehviewer.EhDB.removeLocalFavorites(a)
        // com.hippo.ehviewer.EhDB.removeLocalFavorites(list)
        // com.hippo.ehviewer.EhDB.searchLocalFavorites(keyword)


        EhDB.putLocalFavorites(a)
        EhDB.putLocalFavorites(list)

        var ret = EhDB.getAllLocalFavorites()
        Assert.assertNotNull(ret)
        Assert.assertNotEquals(0, ret.size)

        ret = EhDB.searchLocalFavorites(keyword)

        ret - EhDB.getLocalFavorites(source)
        Assert.assertNotNull(ret)
        Assert.assertNotEquals(0, ret.size)

        var found = EhDB.containLocalFavorites(a)
        Assert.assertTrue(found)
        EhDB.removeLocalFavorites(a)

        found = EhDB.containLocalFavorites(a)
        Assert.assertFalse(found)

        EhDB.removeLocalFavorites(list)

        ret = EhDB.getAllLocalFavorites()
        Assert.assertNotNull(ret)
        Assert.assertEquals(0, ret.size)


    }
}