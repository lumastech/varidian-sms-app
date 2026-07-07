package com.varidian.varidiansms.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

/**
 * FCM-free operation mode: a foreground service that polls the outbox
 * every 20 seconds. Ideal for prototypes / LAN deployments where Firebase
 * isn't configured. When you enable FCM, this becomes a redundancy layer —
 * you can stop it and rely on push + the 15-minute worker.
 */
public class PollingForegroundService extends Service {

    private static final String CHANNEL_ID = "gateway_channel";
    private static final long POLL_INTERVAL_MS = 20_000;

    private HandlerThread thread;
    private Handler handler;
    private volatile boolean running = false;

    private final Runnable pollTask = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            SmsProcessor.processPending(getApplicationContext());
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    public static void start(Context context) {
        Intent intent = new Intent(context, PollingForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, PollingForegroundService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(1, buildNotification());

        thread = new HandlerThread("gateway-poller");
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            running = true;
            handler.post(pollTask);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (thread != null) thread.quitSafely();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("VaridianSMS gateway active")
                .setContentText("Listening for outgoing messages…")
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setOngoing(true)
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "VaridianSMS", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}
