package com.varidian.varidiansms.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.varidian.varidiansms.api.ApiClient;
import com.varidian.varidiansms.services.SmsProcessor;

import java.util.concurrent.TimeUnit;

/**
 * Every 15 minutes (matching httpSMS' heartbeat cadence, and WorkManager's
 * minimum period): ping the server and also drain any pending messages —
 * a safety net in case a push or poll was missed.
 */
public class HeartbeatWorker extends Worker {

    private static final String UNIQUE_NAME = "gateway-heartbeat";

    public HeartbeatWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        ApiClient api = new ApiClient(getApplicationContext());
        boolean ok = api.heartbeat();
        SmsProcessor.processPending(getApplicationContext());
        return ok ? Result.success() : Result.retry();
    }

    public static void schedule(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                HeartbeatWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME);
    }
}
