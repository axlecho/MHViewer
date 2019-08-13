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

package com.hippo.ehviewer.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.hippo.app.ListCheckBoxDialogBuilder;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.UrlOpener;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.dao.DownloadLabel;
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.ehviewer.download.DownloadService;
import com.hippo.ehviewer.ui.scene.BaseScene;
import com.hippo.text.Html;
import com.hippo.unifile.UniFile;
import com.hippo.util.ExceptionUtils;
import com.hippo.util.IoThreadPoolExecutor;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.IOUtils;
import com.hippo.yorozuya.collect.LongList;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class CommonOperations {

    private static final String TAG = CommonOperations.class.getSimpleName();

    private static boolean UPDATING;

    public static void checkUpdate(Activity activity, boolean feedback) {
        if (!UPDATING) {
            UPDATING = true;
            new UpdateTask(activity, feedback).executeOnExecutor(IoThreadPoolExecutor.getInstance());
        }
    }

    private static final class UpdateTask extends AsyncTask<Void, Void, JSONObject> {

        private final Activity mActivity;
        private final OkHttpClient mHttpClient;
        private final boolean mFeedback;

        public UpdateTask(Activity activity, boolean feedback) {
            mActivity = activity;
            mHttpClient = EhApplication.getOkHttpClient(activity);
            mFeedback = feedback;
        }

        private JSONObject fetchUpdateInfo(String url) throws IOException, JSONException {
            Log.d(TAG, url);
            Request request = new Request.Builder().url(url).build();
            Response response = mHttpClient.newCall(request).execute();
            return new JSONObject(response.body().string());
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            String url;
            if (Settings.getBetaUpdateChannel()) {
                url = "https://raw.githubusercontent.com/axlecho/MHViewer/api/update_beta.json";
            } else {
                url = "https://raw.githubusercontent.com/axlecho/MHViewer/api/update.json";
            }

            try {
                return fetchUpdateInfo(url);
            } catch (Throwable e2) {
                ExceptionUtils.throwIfFatal(e2);
                return null;
            }
        }

        private void showUpToDateDialog() {
            new AlertDialog.Builder(mActivity)
                    .setMessage(R.string.update_to_date)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }

        private void showUpdateDialog(String versionName, int versionCode, String size, CharSequence info, final String url) {
            new AlertDialog.Builder(mActivity)
                    .setTitle(R.string.update)
                    .setMessage(mActivity.getString(R.string.update_plain, versionName, size, info))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            UrlOpener.openUrl(mActivity, url, false);
                        }
                    })
                    .setNegativeButton(R.string.update_ignore, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.putSkipUpdateVersion(versionCode);
                        }
                    }).show();
        }

        private void handleResult(JSONObject jo) {
            if (null == jo || mActivity.isFinishing()) {
                return;
            }

            String versionName;
            int versionCode;
            String size;
            CharSequence info;
            String url;

            try {
                PackageManager pm = mActivity.getPackageManager();
                PackageInfo pi = pm.getPackageInfo(mActivity.getPackageName(), PackageManager.GET_ACTIVITIES);
                int currentVersionCode = pi.versionCode;
                versionCode = jo.getInt("version_code");
                if (currentVersionCode >= versionCode) {
                    // Update to date
                    if (mFeedback) {
                        showUpToDateDialog();
                    }
                    return;
                }

                versionName = jo.getString("version_name");
                size = FileUtils.humanReadableByteCount(jo.getLong("size"), false);
                info = Html.fromHtml(jo.getString("info"));
                url = jo.getString("url");
            } catch (Throwable e) {
                ExceptionUtils.throwIfFatal(e);
                return;
            }

            if (mFeedback || versionCode != Settings.getSkipUpdateVersion()) {
                showUpdateDialog(versionName, versionCode, size, info, url);
            }
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            try {
                handleResult(jsonObject);
            } finally {
                UPDATING = false;
            }
        }
    }


    public static void addToFavorites(final Activity activity, final GalleryInfo galleryInfo) {
        EhDB.putLocalFavorites(galleryInfo);
    }

    public static void removeFromFavorites(Activity activity, GalleryInfo galleryInfo) {
        EhDB.removeLocalFavorites(galleryInfo);
    }

    public static void startDownload(final MainActivity activity, final GalleryInfo galleryInfo, boolean forceDefault) {
        startDownload(activity, Collections.singletonList(galleryInfo), forceDefault);
    }

    // TODO Add context if activity and context are different style
    public static void startDownload(final MainActivity activity, final List<GalleryInfo> galleryInfos, boolean forceDefault) {

    }

    public static void ensureNoMediaFile(UniFile file) {
        if (null == file) {
            return;
        }

        UniFile noMedia = file.createFile(".nomedia");
        if (null == noMedia) {
            return;
        }

        InputStream is = null;
        try {
            is = noMedia.openInputStream();
        } catch (IOException e) {
            // Ignore
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public static void removeNoMediaFile(UniFile file) {
        if (null == file) {
            return;
        }

        UniFile noMedia = file.subFile(".nomedia");
        if (null != noMedia && noMedia.isFile()) {
            noMedia.delete();
        }
    }
}
