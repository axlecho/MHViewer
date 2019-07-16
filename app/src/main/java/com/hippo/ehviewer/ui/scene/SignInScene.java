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

package com.hippo.ehviewer.ui.scene;

import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.axlecho.api.MHApi;
import com.axlecho.api.pica.PicaApi;
import com.google.android.material.textfield.TextInputLayout;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.util.ExceptionUtils;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.ViewUtils;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public final class SignInScene extends SolidScene implements EditText.OnEditorActionListener,
        View.OnClickListener {
    @Nullable
    private View mProgress;
    @Nullable
    private TextInputLayout mUsernameLayout;
    @Nullable
    private TextInputLayout mPasswordLayout;
    @Nullable
    private EditText mUsername;
    @Nullable
    private EditText mPassword;
    @Nullable
    private View mSignIn;
    @Nullable
    private TextView mSkipSigningIn;

    private Disposable handle;

    @Override
    public boolean needShowLeftDrawer() {
        return false;
    }

    @Nullable
    @Override
    public View onCreateView2(LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_login, container, false);

        View loginForm = ViewUtils.$$(view, R.id.login_form);
        mProgress = ViewUtils.$$(view, R.id.progress);
        mUsernameLayout = (TextInputLayout) ViewUtils.$$(loginForm, R.id.username_layout);
        mUsername = mUsernameLayout.getEditText();
        AssertUtils.assertNotNull(mUsername);
        mPasswordLayout = (TextInputLayout) ViewUtils.$$(loginForm, R.id.password_layout);
        mPassword = mPasswordLayout.getEditText();
        AssertUtils.assertNotNull(mPassword);
        mSignIn = ViewUtils.$$(loginForm, R.id.sign_in);
        mSkipSigningIn = (TextView) ViewUtils.$$(loginForm, R.id.skip_signing_in);

        mSkipSigningIn.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
        mPassword.setOnEditorActionListener(this);
        mSignIn.setOnClickListener(this);
        mSkipSigningIn.setOnClickListener(this);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Show IME
        if (mProgress != null && View.INVISIBLE != mProgress.getVisibility()) {
            showSoftInput(mUsername);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mProgress = null;
        mUsernameLayout = null;
        mPasswordLayout = null;
        mUsername = null;
        mPassword = null;
        mSignIn = null;
        mSkipSigningIn = null;
    }

    private void showProgress(boolean animation) {
        if (null != mProgress && View.VISIBLE != mProgress.getVisibility()) {
            if (animation) {
                mProgress.setAlpha(0.0f);
                mProgress.setVisibility(View.VISIBLE);
                mProgress.animate().alpha(1.0f).setDuration(500).start();
            } else {
                mProgress.setAlpha(1.0f);
                mProgress.setVisibility(View.VISIBLE);
            }
        }
    }

    private void hideProgress() {
        if (null != mProgress) {
            mProgress.setVisibility(View.GONE);
        }
    }


    @Override
    public void onClick(View v) {
        MainActivity activity = getActivity2();
        if (null == activity) {
            return;
        }

        if (mSignIn == v) {
            signIn();
        } else if (mSkipSigningIn == v) {
            // Set gallery size SITE_E if skip sign in
            Settings.putGallerySite(EhUrl.SITE_E);
            redirectTo();
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v == mPassword) {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                signIn();
                return true;
            }
        }

        return false;
    }

    private void signIn() {
        Context context = getContext2();
        MainActivity activity = getActivity2();
        if (null == context || null == activity || null == mUsername || null == mPassword || null == mUsernameLayout ||
                null == mPasswordLayout) {
            return;
        }

        String username = mUsername.getText().toString();
        String password = mPassword.getText().toString();

        if (username.isEmpty()) {
            mUsernameLayout.setError(getString(R.string.error_username_cannot_empty));
            return;
        } else {
            mUsernameLayout.setError(null);
        }

        if (password.isEmpty()) {
            mPasswordLayout.setError(getString(R.string.error_password_cannot_empty));
            return;
        } else {
            mPasswordLayout.setError(null);
        }

        hideSoftInput();
        showProgress(true);

        handle = PicaApi.Companion.getINSTANCE().login(username, password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> redirectTo(), this::onSignInEnd);
    }

    private void redirectTo() {
        Settings.putNeedSignIn(false);
        MainActivity activity = getActivity2();
        if (null != activity) {
            startSceneForCheckStep(CHECK_STEP_SIGN_IN, getArguments());
        }
        finish();
    }

    private void whetherToSkip(Throwable e) {
        Context context = getContext2();
        if (null == context) {
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.sign_in_failed)
                .setMessage(ExceptionUtils.getReadableString(e))
                .setPositiveButton(R.string.get_it, null)
                .show();
    }

    public void onSignInEnd(Throwable e) {
        Context context = getContext2();
        if (null == context) {
            return;
        }

        hideProgress();
        whetherToSkip(e);
    }
}
