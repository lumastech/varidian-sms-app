package com.varidian.varidiansms.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.varidian.varidiansms.api.ApiClient;
import com.varidian.varidiansms.util.AppPrefs;

import java.util.concurrent.Executors;

/**
 * Optional FCM wake-up path. The Laravel backend sends a silent
 * data message {type: "message.send", message_id: ...} the instant a
 * message's dispatch time arrives, so delivery is near-instant instead
 * of waiting for the next poll.
 */
public class GatewayFcmService extends FirebaseMessagingService {
    private static final String TAG = "GatewayFcm";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String type = remoteMessage.getData().get("type");
        Log.i(TAG, "Push received: " + type);

        if ("message.send".equals(type)) {
            Executors.newSingleThreadExecutor().execute(
                    () -> SmsProcessor.processPending(getApplicationContext()));
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.i(TAG, "New FCM token issued");
        if (new AppPrefs(this).isLoggedIn()) {
            Executors.newSingleThreadExecutor().execute(
                    () -> new ApiClient(this).register(token));
        }
    }
}
