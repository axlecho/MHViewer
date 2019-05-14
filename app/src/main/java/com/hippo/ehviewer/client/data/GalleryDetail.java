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

package com.hippo.ehviewer.client.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.axlecho.api.MHComicComment;
import com.axlecho.api.MHComicDetail;

import java.util.ArrayList;
import java.util.Arrays;

public class GalleryDetail extends GalleryInfo {

    public long apiUid = -1L;
    public String apiKey;
    public int torrentCount;
    public String torrentUrl;
    public String archiveUrl;
    public String parent;
    public String visible;
    public String language;
    public String size;
    public String intro;
    public int pages;
    public int favoriteCount;
    public boolean isFavorited;
    public int ratingCount;
    public long updateTime;
    public GalleryChapterGroup[] chapters;
    public GalleryComment[] comments;
    public int previewPages;
    public PreviewSet previewSet;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.torrentCount);
        dest.writeString(this.torrentUrl);
        dest.writeString(this.archiveUrl);
        dest.writeString(this.parent);
        dest.writeString(this.visible);
        dest.writeString(this.language);
        dest.writeString(this.size);
        dest.writeString(this.intro);
        dest.writeInt(this.pages);
        dest.writeInt(this.favoriteCount);
        dest.writeByte(isFavorited ? (byte) 1 : (byte) 0);
        dest.writeInt(this.ratingCount);
        dest.writeLong(this.updateTime);
        dest.writeParcelableArray(this.chapters, 0);
        dest.writeParcelableArray(this.comments, 0);
        dest.writeInt(this.previewPages);
        dest.writeParcelable(previewSet, flags);
    }

    public GalleryDetail() {
    }

    public GalleryDetail(MHComicDetail detail) {
        super(detail.getInfo());
        this.apiUid = -1;
        this.apiKey = "";
        this.archiveUrl = "";
        // this.comments = detail.getComments();
        this.favoriteCount = detail.getFavoriteCount();
        this.intro = detail.getIntro();
        this.isFavorited = detail.isFavorited();
        this.language = "";
        this.pages = detail.getChapterCount();
        this.parent = null;
        this.previewPages = 0;
        this.previewSet = null;
        this.ratingCount = detail.getRatingCount();
        this.updateTime = detail.getUpdateTime();
        this.size = "";
        this.chapters = null;
        this.torrentCount = 0;
        this.torrentUrl = null;
        this.visible = "";
        ArrayList<GalleryComment> comments = new ArrayList<>();
        for(MHComicComment c: detail.getComments()) {
            GalleryComment comment = new GalleryComment();
            // comment.id = c.getId();
            comment.score = c.getScore();
            comment.comment = c.getComment();
            comment.time = c.getTime();
            comment.user = c.getUser();
            comments.add(comment);
        }
        this.comments = comments.toArray(new GalleryComment[comments.size()]);
    }

    protected GalleryDetail(Parcel in) {
        super(in);
        this.torrentCount = in.readInt();
        this.torrentUrl = in.readString();
        this.archiveUrl = in.readString();
        this.parent = in.readString();
        this.visible = in.readString();
        this.language = in.readString();
        this.size = in.readString();
        this.intro = in.readString();
        this.pages = in.readInt();
        this.favoriteCount = in.readInt();
        this.isFavorited = in.readByte() != 0;
        this.ratingCount = in.readInt();
        this.updateTime = in.readLong();
        Parcelable[] array = in.readParcelableArray(GalleryChapterGroup.class.getClassLoader());
        if (array != null) {
            this.chapters = Arrays.copyOf(array, array.length, GalleryChapterGroup[].class);
        } else {
            this.chapters = null;
        }
        array = in.readParcelableArray(GalleryComment.class.getClassLoader());
        if (array != null) {
            this.comments = Arrays.copyOf(array, array.length, GalleryComment[].class);
        } else {
            this.comments = null;
        }
        this.previewPages = in.readInt();
        this.previewSet = in.readParcelable(PreviewSet.class.getClassLoader());
    }

    public static final Creator<GalleryDetail> CREATOR = new Creator<GalleryDetail>() {
        @Override
        public GalleryDetail createFromParcel(Parcel source) {
            return new GalleryDetail(source);
        }

        @Override
        public GalleryDetail[] newArray(int size) {
            return new GalleryDetail[size];
        }
    };
}
