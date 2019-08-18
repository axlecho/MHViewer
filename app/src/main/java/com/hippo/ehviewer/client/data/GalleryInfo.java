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

import androidx.annotation.Nullable;

import com.axlecho.api.MHComicInfo;
import com.hippo.ehviewer.dao.HistoryInfo;
import com.hippo.ehviewer.dao.LocalFavoriteInfo;

public class GalleryInfo implements Parcelable {
    public String gid;
    public String cid;
    public String token;
    public String title;
    public String titleJpn;
    public String thumb;
    public int category;
    public String posted;
    public String uploader;
    public float rating;
    public boolean rated;
    @Nullable
    public String[] simpleTags;
    public int pages;

    public int thumbWidth;
    public int thumbHeight;

    public int spanSize;
    public int spanIndex;
    public int spanGroupIndex;
    public String source;

    public String getId() {
        return gid + "@" + source;
    }

    public String getCid() {
        return gid + "-" + cid + "@" + source;
    }

    /**
     * language from title
     */
    public String simpleLanguage;

    public int favoriteSlot = -2;
    public String favoriteName;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.gid);
        dest.writeString(this.cid);
        dest.writeString(this.token);
        dest.writeString(this.title);
        dest.writeString(this.titleJpn);
        dest.writeString(this.thumb);
        dest.writeInt(this.category);
        dest.writeString(this.posted);
        dest.writeString(this.uploader);
        dest.writeFloat(this.rating);
        dest.writeByte(this.rated ? (byte) 1 : (byte) 0);
        dest.writeString(this.simpleLanguage);
        dest.writeStringArray(this.simpleTags);
        dest.writeInt(this.thumbWidth);
        dest.writeInt(this.thumbHeight);
        dest.writeInt(this.spanSize);
        dest.writeInt(this.spanIndex);
        dest.writeInt(this.spanGroupIndex);
        dest.writeInt(this.favoriteSlot);
        dest.writeString(this.favoriteName);
        dest.writeString(this.source);
    }

    public GalleryInfo() {
    }

    public GalleryInfo(MHComicInfo info) {
        this.gid = info.getGid();
        this.thumb = info.getThumb();
        this.category = info.getCategory();
        this.title = info.getTitle();
        this.pages = 0;
        this.posted = info.getPosted();
        this.rated = info.getRated();
        this.rating = info.getRating();
        this.titleJpn = info.getTitleJpn();
        this.uploader = info.getUploader();
        this.source = info.getSource();
    }

    public GalleryInfo(LocalFavoriteInfo info) {
        this.source = info.getId().split("@")[1];
        this.title = info.title;
        this.rating = info.rating;
        this.category = info.category;
        this.uploader = info.uploader;
        this.titleJpn = info.titleJpn;
        this.rated = info.rated;
        this.posted = info.posted;
        this.pages = info.pages;
        this.thumb = info.thumb;
        this.cid = info.cid;
        this.gid = info.gid;
        this.favoriteName = info.favoriteName;
        this.favoriteSlot = info.favoriteSlot;
        this.simpleLanguage = info.simpleLanguage;
        this.simpleTags = info.simpleTags;
        this.spanGroupIndex = info.spanGroupIndex;
        this.spanIndex = info.spanIndex;
        this.spanSize = info.spanSize;
        this.thumbHeight = info.thumbHeight;
        this.thumbWidth = info.thumbWidth;
        this.token = info.token;
    }

    public GalleryInfo(HistoryInfo info) {
        this.source = info.getId().split("@")[1];
        this.title = info.title;
        this.rating = info.rating;
        this.category = info.category;
        this.uploader = info.uploader;
        this.titleJpn = info.titleJpn;
        this.rated = info.rated;
        this.posted = info.posted;
        this.pages = info.pages;
        this.thumb = info.thumb;
        this.cid = info.cid;
        this.gid = info.gid;
        this.favoriteName = info.favoriteName;
        this.favoriteSlot = info.favoriteSlot;
        this.simpleLanguage = info.simpleLanguage;
        this.simpleTags = info.simpleTags;
        this.spanGroupIndex = info.spanGroupIndex;
        this.spanIndex = info.spanIndex;
        this.spanSize = info.spanSize;
        this.thumbHeight = info.thumbHeight;
        this.thumbWidth = info.thumbWidth;
        this.token = info.token;
    }

    protected GalleryInfo(Parcel in) {
        this.gid = in.readString();
        this.cid = in.readString();
        this.token = in.readString();
        this.title = in.readString();
        this.titleJpn = in.readString();
        this.thumb = in.readString();
        this.category = in.readInt();
        this.posted = in.readString();
        this.uploader = in.readString();
        this.rating = in.readFloat();
        this.rated = in.readByte() != 0;
        this.simpleLanguage = in.readString();
        this.simpleTags = in.createStringArray();
        this.thumbWidth = in.readInt();
        this.thumbHeight = in.readInt();
        this.spanSize = in.readInt();
        this.spanIndex = in.readInt();
        this.spanGroupIndex = in.readInt();
        this.favoriteSlot = in.readInt();
        this.favoriteName = in.readString();
        this.source = in.readString();
    }

    public static final Parcelable.Creator<GalleryInfo> CREATOR = new Parcelable.Creator<GalleryInfo>() {

        @Override
        public GalleryInfo createFromParcel(Parcel source) {
            return new GalleryInfo(source);
        }

        @Override
        public GalleryInfo[] newArray(int size) {
            return new GalleryInfo[size];
        }
    };

}
