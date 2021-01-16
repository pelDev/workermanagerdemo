package com.peldev.workmanagerdemo.workers;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.peldev.workmanagerdemo.Constants;

import java.io.File;

public class CleanupWorker extends Worker {

    public CleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context applicationContext = getApplicationContext();

        // make a notification when the work starts and slows down the work so that it's easy to
        // see each WorkRequests start.
        WorkerUtils.makeStatusNotification("Cleaning up old temporary files", applicationContext);
        WorkerUtils.sleep();

        try {
            File outputDirectory = new File(applicationContext.getFilesDir(),
                    Constants.OUTPUT_PATH);
            if (outputDirectory.exists()) {
                File[] entries = outputDirectory.listFiles();
                if (entries != null && entries.length > 0) {
                    for (File entry : entries) {
                        String name = entry.getName();
                        if (!TextUtils.isEmpty(name) && name.endsWith(".png")) {
                            boolean deleted = entry.delete();
                            Log.i("CLEANING", String.format("Deleted %s - %s",
                                    name, deleted));
                        }
                    }
                }
            }
            return Worker.Result.success();
        } catch (Exception e) {
            Log.e("ERROR", "error cleaning up", e);
            return Worker.Result.failure();
        }
    }
}
