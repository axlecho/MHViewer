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

package com.hippo.ehviewer.client;

import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.axlecho.api.MHComicChapter;
import com.axlecho.api.MHComicDetail;
import com.axlecho.api.MHComicInfo;
import com.axlecho.api.MHMutiItemResult;
import com.axlecho.api.bangumi.BangumiApi;
import com.axlecho.api.MHApi;
import com.google.gson.Gson;
import com.hippo.ehviewer.AppConfig;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.GetText;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.data.GalleryChapter;
import com.hippo.ehviewer.client.data.GalleryComment;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.GalleryChapterGroup;
import com.hippo.ehviewer.client.data.PreviewSet;
import com.hippo.ehviewer.client.exception.CancelledException;
import com.hippo.ehviewer.client.exception.EhException;
import com.hippo.ehviewer.client.exception.NoHAtHClientException;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.parser.ArchiveParser;
import com.hippo.ehviewer.client.parser.FavoritesParser;
import com.hippo.ehviewer.client.parser.ForumsParser;
import com.hippo.ehviewer.client.parser.GalleryApiParser;
import com.hippo.ehviewer.client.parser.GalleryListParser;
import com.hippo.ehviewer.client.parser.GalleryTokenApiParser;
import com.hippo.ehviewer.client.parser.ProfileParser;
import com.hippo.ehviewer.client.parser.RateGalleryParser;
import com.hippo.ehviewer.client.parser.SignInParser;
import com.hippo.ehviewer.client.parser.TorrentParser;
import com.hippo.ehviewer.client.parser.VoteCommentParser;
import com.hippo.network.StatusCodeException;
import com.hippo.util.ExceptionUtils;
import com.hippo.yorozuya.AssertUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EhEngine {

    private static final String TAG = EhEngine.class.getSimpleName();

    private static final String SAD_PANDA_DISPOSITION = "inline; filename=\"sadpanda.jpg\"";
    private static final String SAD_PANDA_TYPE = "image/gif";
    private static final String SAD_PANDA_LENGTH = "9615";

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");

    private static final Pattern PATTERN_NEED_HATH_CLIENT = Pattern.compile("(You must have a H@H client assigned to your account to use this feature\\.)");

    public static EhFilter sEhFilter;

    public static void initialize() {
        sEhFilter = EhFilter.getInstance();
    }

    private static void doThrowException(Call call, int code, @Nullable Headers headers,
                                         @Nullable String body, Throwable e) throws Throwable {
        if (call.isCanceled()) {
            throw new CancelledException();
        }

        // Check sad panda
        if (headers != null && SAD_PANDA_DISPOSITION.equals(headers.get("Content-Disposition")) &&
                SAD_PANDA_TYPE.equals(headers.get("Content-Type")) &&
                SAD_PANDA_LENGTH.equals(headers.get("Content-Length"))) {
            throw new EhException("Sad Panda");
        }

        if (e instanceof ParseException) {
            if (body != null && !body.contains("<")) {
                throw new EhException(body);
            } else {
                if (Settings.getSaveParseErrorBody()) {
                    AppConfig.saveParseErrorBody((ParseException) e);
                }
                throw new EhException(GetText.getString(R.string.error_parse_error));
            }
        }

        if (code >= 400) {
            throw new StatusCodeException(code);
        }

        if (e != null) {
            throw e;
        }
    }

    private static void throwException(Call call, int code, @Nullable Headers headers,
                                       @Nullable String body, Throwable e) throws Throwable {
        try {
            doThrowException(call, code, headers, body, e);
        } catch (Throwable error) {
            error.printStackTrace();
            throw error;
        }
    }

    public static String signIn(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
                                String username, String password, String recaptchaChallenge, String recaptchaResponse) throws Throwable {
        FormBody.Builder builder = new FormBody.Builder()
                .add("UserName", username)
                .add("PassWord", password)
                .add("submit", "Log me in")
                .add("CookieDate", "1")
                .add("temporary_https", "off");
        if (!TextUtils.isEmpty(recaptchaChallenge) && !TextUtils.isEmpty(recaptchaResponse)) {
            builder.add("recaptcha_challenge_field", recaptchaChallenge);
            builder.add("recaptcha_response_field", recaptchaResponse);
        }
        String url = EhUrl.API_SIGN_IN;
        String referer = "https://forums.e-hentai.org/index.php?act=Login&CODE=00";
        String origin = "https://forums.e-hentai.org";
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, referer, origin)
                .post(builder.build())
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return SignInParser.parse(body);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    private static void fillGalleryList(@Nullable EhClient.Task task, OkHttpClient okHttpClient, List<GalleryInfo> list, String url, boolean filter) throws Throwable {
        // Filter title and uploader
        if (filter) {
            for (int i = 0, n = list.size(); i < n; i++) {
                GalleryInfo info = list.get(i);
                if (!sEhFilter.filterTitle(info) || !sEhFilter.filterUploader(info)) {
                    list.remove(i);
                    i--;
                    n--;
                }
            }
        }

        boolean hasTags = false;
        boolean hasPages = false;
        boolean hasRated = false;
        for (GalleryInfo gi : list) {
            if (gi.simpleTags != null) {
                hasTags = true;
            }
            if (gi.pages != 0) {
                hasPages = true;
            }
            if (gi.rated) {
                hasRated = true;
            }
        }

        boolean needApi = (filter && sEhFilter.needTags() && !hasTags) ||
                (Settings.getShowGalleryPages() && !hasPages) ||
                hasRated;
        if (needApi) {
            fillGalleryListByApi(task, okHttpClient, list, url);
        }

        // Filter tag
        if (filter) {
            for (int i = 0, n = list.size(); i < n; i++) {
                GalleryInfo info = list.get(i);
                if (!sEhFilter.filterTag(info) || !sEhFilter.filterTagNamespace(info)) {
                    list.remove(i);
                    i--;
                    n--;
                }
            }
        }

        for (GalleryInfo info : list) {
            info.thumb = EhUrl.getFixedPreviewThumbUrl(info.thumb);
        }
    }

    public static GalleryListParser.Result getGalleryList(@Nullable EhClient.Task task, OkHttpClient okHttpClient, String type,int page,
                                                          String time,String category) throws Throwable {
        MHMutiItemResult<MHComicInfo> comics;
        if(type.equals("top")) {
            comics = MHApi.Companion.getINSTANCE().category().time(time).category(category).top(page).blockingFirst();
        } else {
            comics = MHApi.Companion.getINSTANCE().recent(page).blockingFirst();
        }

        GalleryListParser.Result result = new GalleryListParser.Result();
        result.pages = comics.getPages();
        result.nextPage = comics.getCurrentPage() + 1;
        result.noWatchedTags = false;
        List<GalleryInfo> list = new ArrayList<>(comics.getDatas().size());
        for (MHComicInfo comic : comics.getDatas()) {
            list.add(new GalleryInfo(comic));
        }
        result.galleryInfoList = list;
        return result;
    }

    public static GalleryListParser.Result search(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
                                                  String keyword,int page) {
        GalleryListParser.Result result = new GalleryListParser.Result();

        MHMutiItemResult<MHComicInfo> comics = MHApi.Companion.getINSTANCE().search(keyword,page).blockingFirst();
        result.pages = comics.getPages();
        result.nextPage = comics.getCurrentPage() + 1;
        result.noWatchedTags = false;
        List<GalleryInfo> list = new ArrayList<>(comics.getDatas().size());
        for (MHComicInfo comic : comics.getDatas()) {
            list.add(new GalleryInfo((comic)));
        }
        result.galleryInfoList = list;
        return result;
    }

    // At least, GalleryInfo contain valid gid and token
    public static List<GalleryInfo> fillGalleryListByApi(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
                                                         List<GalleryInfo> galleryInfoList, String referer) throws Throwable {
        // We can only request 25 items one time at most
        final int MAX_REQUEST_SIZE = 25;
        List<GalleryInfo> requestItems = new ArrayList<>(MAX_REQUEST_SIZE);
        for (int i = 0, size = galleryInfoList.size(); i < size; i++) {
            requestItems.add(galleryInfoList.get(i));
            if (requestItems.size() == MAX_REQUEST_SIZE || i == size - 1) {
                doFillGalleryListByApi(task, okHttpClient, requestItems, referer);
                requestItems.clear();
            }
        }
        return galleryInfoList;
    }

    private static void doFillGalleryListByApi(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
                                               List<GalleryInfo> galleryInfoList, String referer) throws Throwable {
        JSONObject json = new JSONObject();
        json.put("method", "gdata");
        JSONArray ja = new JSONArray();
        for (int i = 0, size = galleryInfoList.size(); i < size; i++) {
            GalleryInfo gi = galleryInfoList.get(i);
            JSONArray g = new JSONArray();
            g.put(gi.gid);
            g.put(gi.token);
            ja.put(g);
        }
        json.put("gidlist", ja);
        json.put("namespace", 1);
        String url = EhUrl.getApiUrl();
        String origin = EhUrl.getOrigin();
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, referer, origin)
                .post(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            GalleryApiParser.parse(body, galleryInfoList);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static GalleryDetail getGalleryDetail(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
                                                 String gid) throws Throwable {
        MHComicDetail info = MHApi.Companion.getINSTANCE().info(gid).blockingFirst();
        GalleryDetail detail = new GalleryDetail(info);

        List<GalleryChapterGroup> list = new ArrayList<>();
        GalleryChapterGroup g = new GalleryChapterGroup("章节",new ArrayList<>());
        for (MHComicChapter c : info.getChapters()) {
            g.addChapter(new GalleryChapter(c.getTitle(),c.getUrl(),false));
        }
        list.add(g);
        detail.chapters = list.toArray(new GalleryChapterGroup[list.size()]);
        return detail;
    }


    public static Pair<PreviewSet, Integer> getPreviewSet(
            @Nullable EhClient.Task task, OkHttpClient okHttpClient, String url) throws Throwable {
        return new Pair<>(null,null);

    }

    public static RateGalleryParser.Result rateGallery(@Nullable EhClient.Task task,
                                                       OkHttpClient okHttpClient, long apiUid, String apiKey, long gid,
                                                       String token, float rating) throws Throwable {
        return null;
    }

    public static GalleryComment[] commentGallery(@Nullable EhClient.Task task,
                                                  OkHttpClient okHttpClient, String url, String comment) throws Throwable {
        return new GalleryComment[0];
    }

    public static String getGalleryToken(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
                                         long gid, String gtoken, int page) throws Throwable {
        JSONObject json = new JSONObject()
                .put("method", "gtoken")
                .put("pagelist", new JSONArray().put(
                        new JSONArray().put(gid).put(gtoken).put(page + 1)));
        final RequestBody requestBody = RequestBody.create(MEDIA_TYPE_JSON, json.toString());
        String url = EhUrl.getApiUrl();
        String referer = EhUrl.getReferer();
        String origin = EhUrl.getOrigin();
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, referer, origin)
                .post(requestBody)
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return GalleryTokenApiParser.parse(body);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static FavoritesParser.Result getFavorites(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
                                                      int page) throws Throwable {

        MHMutiItemResult<MHComicInfo>  comics = MHApi.Companion.getINSTANCE().collection("axlecho",page).blockingFirst();
        FavoritesParser.Result result = new FavoritesParser.Result();
        result.pages = comics.getPages();
        result.nextPage = comics.getCurrentPage() + 1;
        result.galleryInfoList = new ArrayList<>();
        result.catArray = new String[]{"想读","1","2","3","4","5","6","7","8","9"};
        result.countArray = new int[10];
        result.countArray[0] = 53;

        for (MHComicInfo c : comics.getDatas()) {
            result.galleryInfoList.add(new GalleryInfo(c));
        }
        return result;
    }

    /**
     * @param dstCat -1 for delete, 0 - 9 for cloud favorite, others throw Exception
     * @param note   max 250 characters
     */
    public static Void addFavorites(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
                                    long gid, String token, int dstCat, String note) throws Throwable   {
        String catStr;
        if (dstCat == -1) {
            catStr = "favdel";
        } else if (dstCat >= 0 && dstCat <= 9) {
            catStr = String.valueOf(dstCat);
        } else {
            throw new EhException("Invalid dstCat: " + dstCat);
        }
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("favcat", catStr);
        builder.add("favnote", note != null ? note : "");
        // submit=Add+to+Favorites is not necessary, just use submit=Apply+Changes all the time
        builder.add("submit", "Apply Changes");
        builder.add("update", "1");
        String url = EhUrl.getAddFavorites(gid, token);
        String origin = EhUrl.getOrigin();
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, url, origin)
                .post(builder.build())
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            throwException(call, code, headers, body, null);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }

        return null;
    }

    public static Void addFavoritesRange(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
                                         long[] gidArray, String[] tokenArray, int dstCat) throws Throwable {
        AssertUtils.assertEquals(gidArray.length, tokenArray.length);
        for (int i = 0, n = gidArray.length; i < n; i++) {
            addFavorites(task, okHttpClient, gidArray[i], tokenArray[i], dstCat, null);
        }
        return null;
    }

    public static FavoritesParser.Result modifyFavorites(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
                                                         String url, long[] gidArray, int dstCat, boolean callApi) throws Throwable {

        return new FavoritesParser.Result();
    }

    public static Pair<String, String>[] getTorrentList(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
                                                        String url, long gid, String token) throws Throwable {
        return null;
    }

    public static Pair<String, Pair<String, String>[]> getArchiveList(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
                                                                      String url, long gid, String token) throws Throwable {
       return null;
    }

    public static Void downloadArchive(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
                                       long gid, String token, String or, String res) throws Throwable {
        return null;
    }

    private static ProfileParser.Result getProfileInternal(@Nullable EhClient.Task task,
                                                           OkHttpClient okHttpClient, String url, String referer) throws Throwable {
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, referer).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return ProfileParser.parse(body);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static ProfileParser.Result getProfile(@Nullable EhClient.Task task,
                                                  OkHttpClient okHttpClient) throws Throwable {
        String url = EhUrl.URL_FORUMS;
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return getProfileInternal(task, okHttpClient, ForumsParser.parse(body), url);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static VoteCommentParser.Result voteComment(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
                                                       long apiUid, String apiKey, long gid, String token, long commentId, int commentVote) throws Throwable {
        final JSONObject json = new JSONObject();
        json.put("method", "votecomment");
        json.put("apiuid", apiUid);
        json.put("apikey", apiKey);
        json.put("gid", gid);
        json.put("token", token);
        json.put("comment_id", commentId);
        json.put("comment_vote", commentVote);
        final RequestBody requestBody = RequestBody.create(MEDIA_TYPE_JSON, json.toString());
        String url = EhUrl.getApiUrl();
        String referer = EhUrl.getReferer();
        String origin = EhUrl.getOrigin();
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, referer, origin)
                .post(requestBody)
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return VoteCommentParser.parse(body, commentVote);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    /**
     * @param image Must be jpeg
     */
    public static GalleryListParser.Result imageSearch(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
                                                       File image, boolean uss, boolean osc, boolean se) throws Throwable {
        return new GalleryListParser.Result();
    }

}
