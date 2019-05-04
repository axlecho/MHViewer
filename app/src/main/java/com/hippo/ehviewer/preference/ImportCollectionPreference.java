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

package com.hippo.ehviewer.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.ui.scene.GalleryListScene;
import com.hippo.preference.MessagePreference;

public class ImportCollectionPreference extends MessagePreference {

    public ImportCollectionPreference(Context context) {
        super(context);
        init();
    }

    public ImportCollectionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ImportCollectionPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setDialogMessage(getContext().getString(R.string.settings_eh_import_collection_text));
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setPositiveButton(R.string.settings_eh_import_collection_tip_yes, this);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            Toast.makeText(getContext(), "test", Toast.LENGTH_SHORT).show();
            EhClient mClient = EhApplication.getEhClient(getContext());
            EhRequest request = new EhRequest();
            request.setMethod(EhClient.METHOD_IMPORT_COLLECTION);
            //request.setCallback(new GalleryListScene.GetGalleryListListener(getContext(),
            //         activity.getStageId(), getTag(), taskId));
            request.setArgs("axlecho");
            mClient.execute(request);
        }
    }
}
