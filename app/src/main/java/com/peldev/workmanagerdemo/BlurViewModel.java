/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.peldev.workmanagerdemo;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.app.Application;
import android.net.Uri;
import android.text.TextUtils;

import com.peldev.workmanagerdemo.workers.BlurWorker;
import com.peldev.workmanagerdemo.workers.CleanupWorker;
import com.peldev.workmanagerdemo.workers.SaveImageToFileWorker;

import java.util.List;

import static com.peldev.workmanagerdemo.Constants.*;

public class BlurViewModel extends AndroidViewModel {

    private Uri mImageUri;
    private WorkManager mWorkManager;

    public Uri getOutputUri() {
        return mOutputUri;
    }

    public void setOutputUri(String outputUri) {
        mOutputUri = uriOrNull(outputUri);
    }

    private Uri mOutputUri;

    public LiveData<List<WorkInfo>> getSavedWorkInfo() {
        return mSavedWorkInfo;
    }


    // cancel running work
    public void cancelWork() {
        mWorkManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME);
    }

    // new instance variable for the work info class
    private LiveData<List<WorkInfo>> mSavedWorkInfo;

    public BlurViewModel(@NonNull Application application) {
        super(application);
        mWorkManager = WorkManager.getInstance(application);
        mSavedWorkInfo = mWorkManager.getWorkInfosByTagLiveData(TAG_OUTPUT);
    }

    void applyBlur(int blurLevel) {
        // create charging constraint
        Constraints constraints = new Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresStorageNotLow(true)
                .build();

        // add work request to clean up temporary images
        WorkContinuation continuation =
                mWorkManager.beginUniqueWork(
                        IMAGE_MANIPULATION_WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequest.from(CleanupWorker.class));

        // add work request to blur image the number of times requested
        for (int i = 0; i < blurLevel; i++) {
            OneTimeWorkRequest.Builder blurRequest =
                    new OneTimeWorkRequest.Builder(BlurWorker.class);

            if (i == 0) {
                blurRequest.setInputData(createInputDataForUri());
            }

            continuation = continuation.then(blurRequest.build());
        }

        // add work request to save image to gallery
        OneTimeWorkRequest saveRequest =
                new OneTimeWorkRequest.Builder(SaveImageToFileWorker.class)
                        .addTag(TAG_OUTPUT)
                        .setConstraints(constraints)
                        .build();
        continuation = continuation.then(saveRequest);
        continuation.enqueue();
    }

    private Data createInputDataForUri() {
        Data.Builder builder = new Data.Builder();
        if (mImageUri != null) {
            builder.putString(KEY_IMAGE_URI, mImageUri.toString());
        }
        return builder.build();
    }

    private Uri uriOrNull(String uriString) {
        if (!TextUtils.isEmpty(uriString)) {
            return Uri.parse(uriString);
        }
        return null;
    }

    void setImageUri(String uri) {
        mImageUri = uriOrNull(uri);
    }

    Uri getImageUri() {
        return mImageUri;
    }

}