/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.dao.DaoMaster;
import com.hippo.ehviewer.dao.DaoSession;
import com.hippo.ehviewer.dao.DownloadDirname;
import com.hippo.ehviewer.dao.DownloadDirnameDao;
import com.hippo.ehviewer.dao.DownloadInfo;
import com.hippo.ehviewer.dao.DownloadLabel;
import com.hippo.ehviewer.dao.DownloadLabelDao;
import com.hippo.ehviewer.dao.DownloadsDao;
import com.hippo.ehviewer.dao.Filter;
import com.hippo.ehviewer.dao.FilterDao;
import com.hippo.ehviewer.dao.HistoryDao;
import com.hippo.ehviewer.dao.HistoryInfo;
import com.hippo.ehviewer.dao.QuickSearch;
import com.hippo.ehviewer.dao.QuickSearchDao;
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.ehviewer.persistence.LocalFavoriteInfo;
import com.hippo.ehviewer.persistence.MHDatabase;
import com.hippo.ehviewer.persistence.ReadingRecord;
import com.hippo.util.ExceptionUtils;
import com.hippo.util.SqlUtils;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.IOUtils;
import com.hippo.yorozuya.ObjectUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.dao.query.LazyList;

public class EhDB {

    private static final String TAG = EhDB.class.getSimpleName();

    private static final int MAX_HISTORY_COUNT = 100;

    private static DaoSession sDaoSession;

    private static class DBOpenHelper extends DaoMaster.OpenHelper {

        public DBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
            super(context, name, factory);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            upgradeDB(db, oldVersion);
        }
    }

    private static Context sContext;

    private static void upgradeDB(SQLiteDatabase db, int oldVersion) {
        switch (oldVersion) {
            case 1: // 1 to 2, add FILTER
                FilterDao.createTable(db, true);
            case 2: // 2 to 3, add ENABLE column to table FILTER
                db.execSQL("CREATE TABLE " + "\"FILTER2\" (" +
                        "\"_id\" INTEGER PRIMARY KEY ," +
                        "\"MODE\" INTEGER NOT NULL ," +
                        "\"TEXT\" TEXT," +
                        "\"ENABLE\" INTEGER);");
                db.execSQL("INSERT INTO \"FILTER2\" (" +
                        "_id, MODE, TEXT, ENABLE)" +
                        "SELECT _id, MODE, TEXT, 1 FROM FILTER;");
                db.execSQL("DROP TABLE FILTER");
                db.execSQL("ALTER TABLE FILTER2 RENAME TO FILTER");
            case 3: // 3 to 4, add PAGE_FROM and PAGE_TO column to QUICK_SEARCH
                db.execSQL("CREATE TABLE " + "\"QUICK_SEARCH2\" (" +
                        "\"_id\" INTEGER PRIMARY KEY ," +
                        "\"NAME\" TEXT," +
                        "\"MODE\" INTEGER NOT NULL ," +
                        "\"CATEGORY\" INTEGER NOT NULL ," +
                        "\"KEYWORD\" TEXT," +
                        "\"ADVANCE_SEARCH\" INTEGER NOT NULL ," +
                        "\"MIN_RATING\" INTEGER NOT NULL ," +
                        "\"PAGE_FROM\" INTEGER NOT NULL ," +
                        "\"PAGE_TO\" INTEGER NOT NULL ," +
                        "\"TIME\" INTEGER NOT NULL );");
                db.execSQL("INSERT INTO \"QUICK_SEARCH2\" (" +
                        "_id, NAME, MODE, CATEGORY, KEYWORD, ADVANCE_SEARCH, MIN_RATING, PAGE_FROM, PAGE_TO, TIME)" +
                        "SELECT _id, NAME, MODE, CATEGORY, KEYWORD, ADVANCE_SEARCH, MIN_RATING, -1, -1, TIME FROM QUICK_SEARCH;");
                db.execSQL("DROP TABLE QUICK_SEARCH");
                db.execSQL("ALTER TABLE QUICK_SEARCH2 RENAME TO QUICK_SEARCH");
        }
    }

    public static void initialize(Context context) {
        DBOpenHelper helper = new DBOpenHelper(context.getApplicationContext(), "eh.db", null);

        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);

        sDaoSession = daoMaster.newSession();
        sContext = context.getApplicationContext();
    }


    public static synchronized List<DownloadInfo> getAllDownloadInfo() {
        DownloadsDao dao = sDaoSession.getDownloadsDao();
        List<DownloadInfo> list = dao.queryBuilder().orderDesc(DownloadsDao.Properties.Time).list();
        // Fix state
        for (DownloadInfo info : list) {
            if (info.state == DownloadInfo.STATE_WAIT || info.state == DownloadInfo.STATE_DOWNLOAD) {
                info.state = DownloadInfo.STATE_NONE;
            }
        }
        return list;
    }

    // Insert or update
    public static synchronized void putDownloadInfo(DownloadInfo downloadInfo) {
        DownloadsDao dao = sDaoSession.getDownloadsDao();
        if (null != dao.load(downloadInfo.gid)) {
            // Update
            dao.update(downloadInfo);
        } else {
            // Insert
            dao.insert(downloadInfo);
        }
    }

    public static synchronized void removeDownloadInfo(String gid) {
        sDaoSession.getDownloadsDao().deleteByKey(gid);
    }

    @Nullable
    public static synchronized String getDownloadDirname(String gid) {
        DownloadDirnameDao dao = sDaoSession.getDownloadDirnameDao();
        DownloadDirname raw = dao.load(gid);
        if (raw != null) {
            return raw.getDirname();
        } else {
            return null;
        }
    }

    /**
     * Insert or update
     */
    public static synchronized void putDownloadDirname(String gid, String dirname) {
        DownloadDirnameDao dao = sDaoSession.getDownloadDirnameDao();
        DownloadDirname raw = dao.load(gid);
        if (raw != null) { // Update
            raw.setDirname(dirname);
            dao.update(raw);
        } else { // Insert
            raw = new DownloadDirname();
            raw.setGid(gid);
            raw.setDirname(dirname);
            dao.insert(raw);
        }
    }

    public static synchronized void removeDownloadDirname(String gid) {
        DownloadDirnameDao dao = sDaoSession.getDownloadDirnameDao();
        dao.deleteByKey(gid);
    }

    public static synchronized void clearDownloadDirname() {
        DownloadDirnameDao dao = sDaoSession.getDownloadDirnameDao();
        dao.deleteAll();
    }

    @NonNull
    public static synchronized List<DownloadLabel> getAllDownloadLabelList() {
        DownloadLabelDao dao = sDaoSession.getDownloadLabelDao();
        return dao.queryBuilder().orderAsc(DownloadLabelDao.Properties.Time).list();
    }

    public static synchronized DownloadLabel addDownloadLabel(String label) {
        DownloadLabelDao dao = sDaoSession.getDownloadLabelDao();
        DownloadLabel raw = new DownloadLabel();
        raw.setLabel(label);
        raw.setTime(System.currentTimeMillis());
        raw.setId(dao.insert(raw));
        return raw;
    }

    public static synchronized DownloadLabel addDownloadLabel(DownloadLabel raw) {
        // Reset id
        raw.setId(null);
        DownloadLabelDao dao = sDaoSession.getDownloadLabelDao();
        raw.setId(dao.insert(raw));
        return raw;
    }

    public static synchronized void updateDownloadLabel(DownloadLabel raw) {
        DownloadLabelDao dao = sDaoSession.getDownloadLabelDao();
        dao.update(raw);
    }

    public static synchronized void moveDownloadLabel(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }

        boolean reverse = fromPosition > toPosition;
        int offset = reverse ? toPosition : fromPosition;
        int limit = reverse ? fromPosition - toPosition + 1 : toPosition - fromPosition + 1;

        DownloadLabelDao dao = sDaoSession.getDownloadLabelDao();
        List<DownloadLabel> list = dao.queryBuilder().orderAsc(DownloadLabelDao.Properties.Time)
                .offset(offset).limit(limit).list();

        int step = reverse ? 1 : -1;
        int start = reverse ? limit - 1 : 0;
        int end = reverse ? 0 : limit - 1;
        long toTime = list.get(end).getTime();
        for (int i = end; reverse ? i < start : i > start; i += step) {
            list.get(i).setTime(list.get(i + step).getTime());
        }
        list.get(start).setTime(toTime);

        dao.updateInTx(list);
    }

    public static synchronized void removeDownloadLabel(DownloadLabel raw) {
        DownloadLabelDao dao = sDaoSession.getDownloadLabelDao();
        dao.delete(raw);
    }

    public static synchronized List<GalleryInfo> getAllLocalFavorites() {
        List<LocalFavoriteInfo> list = MHDatabase.Companion.getInstance(sContext).favorite().list();
        List<GalleryInfo> result = new ArrayList<>();
        for (LocalFavoriteInfo info : list) {
            result.add(new GalleryInfo(info));
        }
        return result;
    }

    public static synchronized List<GalleryInfo> getLocalFavorites(String source) {
        source = SqlUtils.sqlEscapeString("%" + "@" + source + "%");
        List<LocalFavoriteInfo> list = MHDatabase.Companion.getInstance(sContext).favorite().list(source);
        List<GalleryInfo> result = new ArrayList<>();
        for (LocalFavoriteInfo info : list) {
            result.add(new GalleryInfo(info));
        }
        return result;
    }

    public static synchronized List<GalleryInfo> searchLocalFavorites(String query) {
        query = SqlUtils.sqlEscapeString("%" + query + "%");
        List<LocalFavoriteInfo> list = MHDatabase.Companion.getInstance(sContext).favorite().search(query);
        List<GalleryInfo> result = new ArrayList<>();
        for (LocalFavoriteInfo info : list) {
            result.add(new GalleryInfo(info));
        }
        return result;
    }

    public static synchronized void removeLocalFavorites(GalleryInfo info) {
        MHDatabase.Companion.getInstance(sContext).favorite().delete(info.getId());
    }

    public static synchronized void removeLocalFavorites(GalleryInfo[] infoArray) {
        for (GalleryInfo info : infoArray) {
            MHDatabase.Companion.getInstance(sContext).favorite().delete(info.getId());
        }
    }

    public static synchronized boolean containLocalFavorites(GalleryInfo info) {
        return MHDatabase.Companion.getInstance(sContext).favorite().load(info.getId()) == null;
    }

    public static synchronized void putLocalFavorites(GalleryInfo galleryInfo) {
        MHDatabase.Companion.getInstance(sContext).favorite().insert(new LocalFavoriteInfo(galleryInfo));
    }

    public static synchronized void putLocalFavorites(List<GalleryInfo> galleryInfoList) {
        for (GalleryInfo gi : galleryInfoList) {
            putLocalFavorites(gi);
        }
    }

    public static synchronized List<QuickSearch> getAllQuickSearch() {
        QuickSearchDao dao = sDaoSession.getQuickSearchDao();
        return dao.queryBuilder().orderAsc(QuickSearchDao.Properties.Time).list();
    }

    public static synchronized void insertQuickSearch(QuickSearch quickSearch) {
        QuickSearchDao dao = sDaoSession.getQuickSearchDao();
        quickSearch.id = null;
        quickSearch.time = System.currentTimeMillis();
        quickSearch.id = dao.insert(quickSearch);
    }

    public static synchronized void updateQuickSearch(QuickSearch quickSearch) {
        QuickSearchDao dao = sDaoSession.getQuickSearchDao();
        dao.update(quickSearch);
    }

    public static synchronized void deleteQuickSearch(QuickSearch quickSearch) {
        QuickSearchDao dao = sDaoSession.getQuickSearchDao();
        dao.delete(quickSearch);
    }

    public static synchronized void moveQuickSearch(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }

        boolean reverse = fromPosition > toPosition;
        int offset = reverse ? toPosition : fromPosition;
        int limit = reverse ? fromPosition - toPosition + 1 : toPosition - fromPosition + 1;

        QuickSearchDao dao = sDaoSession.getQuickSearchDao();
        List<QuickSearch> list = dao.queryBuilder().orderAsc(QuickSearchDao.Properties.Time)
                .offset(offset).limit(limit).list();

        int step = reverse ? 1 : -1;
        int start = reverse ? limit - 1 : 0;
        int end = reverse ? 0 : limit - 1;
        long toTime = list.get(end).getTime();
        for (int i = end; reverse ? i < start : i > start; i += step) {
            list.get(i).setTime(list.get(i + step).getTime());
        }
        list.get(start).setTime(toTime);

        dao.updateInTx(list);
    }


    public static synchronized LazyList<HistoryInfo> getHistoryLazyList() {
        return sDaoSession.getHistoryDao().queryBuilder().orderDesc(HistoryDao.Properties.Time).listLazy();
    }

    public static synchronized void putHistoryInfo(GalleryInfo galleryInfo) {
        HistoryDao dao = sDaoSession.getHistoryDao();
        HistoryInfo info = dao.load(galleryInfo.getId());
        if (null != info) {
            // Update time
            info.time = System.currentTimeMillis();
            info.setId(galleryInfo.getId());
            dao.update(info);
        } else {
            // New history
            info = new HistoryInfo(galleryInfo);
            info.time = System.currentTimeMillis();
            dao.insert(info);
            List<HistoryInfo> list = dao.queryBuilder().orderDesc(HistoryDao.Properties.Time)
                    .limit(-1).offset(MAX_HISTORY_COUNT).list();
            dao.deleteInTx(list);
        }
    }

    public static synchronized void putHistoryInfo(List<HistoryInfo> historyInfoList) {
        HistoryDao dao = sDaoSession.getHistoryDao();
        for (HistoryInfo info : historyInfoList) {
            if (null == dao.load(info.gid)) {
                dao.insert(info);
            }
        }

        List<HistoryInfo> list = dao.queryBuilder().orderDesc(HistoryDao.Properties.Time)
                .limit(-1).offset(MAX_HISTORY_COUNT).list();
        dao.deleteInTx(list);
    }

    public static synchronized void deleteHistoryInfo(HistoryInfo info) {
        HistoryDao dao = sDaoSession.getHistoryDao();
        dao.delete(info);
    }

    public static synchronized void clearHistoryInfo() {
        HistoryDao dao = sDaoSession.getHistoryDao();
        dao.deleteAll();
    }

    public static synchronized ReadingRecord getReadingRecord(String id) {
        return MHDatabase.Companion.getInstance(sContext).readingRecord().load(id);
    }

    public static synchronized void putReadingRecord(ReadingRecord record) {
        MHDatabase.Companion.getInstance(sContext).readingRecord().insert(record);
    }


    public static synchronized List<Filter> getAllFilter() {
        return sDaoSession.getFilterDao().queryBuilder().list();
    }

    public static synchronized void addFilter(Filter filter) {
        filter.setId(null);
        filter.setId(sDaoSession.getFilterDao().insert(filter));
    }

    public static synchronized void deleteFilter(Filter filter) {
        sDaoSession.getFilterDao().delete(filter);
    }

    public static synchronized void triggerFilter(Filter filter) {
        filter.setEnable(!filter.enable);
        sDaoSession.getFilterDao().update(filter);
    }

    public static synchronized boolean exportDB(Context context, File file) {
        File dbFile = context.getDatabasePath("eh.db");
        if (null == dbFile || !dbFile.isFile()) {
            return false;
        }
        if (null == file || !FileUtils.ensureFile(file)) {
            return false;
        }
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(dbFile);
            os = new FileOutputStream(file);
            IOUtils.copy(is, os);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
        // Delete failed file
        file.delete();
        return false;
    }

    public static synchronized String importDBRecordAndFavorite(Context context, File file) {
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            DaoMaster daoMaster = new DaoMaster(db);
            DaoSession session = daoMaster.newSession();


            // LocalFavorites
//            List<LocalFavoriteInfo> localFavoriteInfoList = session.getLocalFavoritesDao().queryBuilder().list();
//            for (LocalFavoriteInfo info : localFavoriteInfoList) {
//                putLocalFavorites(info);
//            }

//            List<ReadingRecord> readingRecordList = MHDatabase.Companion.getInstance(sContext).readingRecord().list();
//            for (ReadingRecord record : readingRecordList) {
//                putReadingRecord(record);
//            }

            return null;
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            // Ignore
            return context.getString(R.string.cant_read_the_file);
        }
    }

    /**
     * @param file The db file
     * @return error string, null for no error
     */
    public static synchronized String importDB(Context context, File file) {
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(
                    file.getPath(), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            int newVersion = DaoMaster.SCHEMA_VERSION;
            int oldVersion = db.getVersion();
            if (oldVersion < newVersion) {
                upgradeDB(db, oldVersion);
                db.setVersion(newVersion);
            } else if (oldVersion > newVersion) {
                return context.getString(R.string.cant_read_the_file);
            }

            DaoMaster daoMaster = new DaoMaster(db);
            DaoSession session = daoMaster.newSession();

            // Downloads
            DownloadManager manager = EhApplication.getDownloadManager(context);
            List<DownloadInfo> downloadInfoList = session.getDownloadsDao().queryBuilder().list();
            manager.addDownload(downloadInfoList);

            // Download label
            List<DownloadLabel> downloadLabelList = session.getDownloadLabelDao().queryBuilder().list();
            manager.addDownloadLabel(downloadLabelList);

            // Download dirname
            List<DownloadDirname> downloadDirnameList = session.getDownloadDirnameDao().queryBuilder().list();
            for (DownloadDirname dirname : downloadDirnameList) {
                putDownloadDirname(dirname.getGid(), dirname.getDirname());
            }

            // History
            List<HistoryInfo> historyInfoList = session.getHistoryDao().queryBuilder().list();
            putHistoryInfo(historyInfoList);

            // QuickSearch
            List<QuickSearch> quickSearchList = session.getQuickSearchDao().queryBuilder().list();
            List<QuickSearch> currentQuickSearchList = sDaoSession.getQuickSearchDao().queryBuilder().list();
            for (QuickSearch quickSearch : quickSearchList) {
                String name = quickSearch.name;
                for (QuickSearch q : currentQuickSearchList) {
                    if (ObjectUtils.equal(q.name, name)) {
                        // The same name
                        name = null;
                        break;
                    }
                }
                if (null == name) {
                    continue;
                }
                insertQuickSearch(quickSearch);
            }

            // LocalFavorites
//            List<LocalFavoriteInfo> localFavoriteInfoList = session.getLocalFavoritesDao().queryBuilder().list();
//            for (LocalFavoriteInfo info : localFavoriteInfoList) {
//                putLocalFavorites(info);
//            }

            // Bookmarks
            // TODO

//            List<ReadingRecord> readingRecordList = session.getReadingRecordDao().queryBuilder().list();
//            for (ReadingRecord record : readingRecordList) {
//                putReadingRecord(record);
//            }

            // Filter
            List<Filter> filterList = session.getFilterDao().queryBuilder().list();
            List<Filter> currentFilterList = sDaoSession.getFilterDao().queryBuilder().list();
            for (Filter filter : filterList) {
                if (!currentFilterList.contains(filter)) {
                    addFilter(filter);
                }
            }

            return null;
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            // Ignore
            return context.getString(R.string.cant_read_the_file);
        }
    }
}
