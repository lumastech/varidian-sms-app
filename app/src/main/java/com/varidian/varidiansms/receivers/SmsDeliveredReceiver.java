package com.varidian.varidiansms.receivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.varidian.varidiansms.api.ApiClient;
import com.varidian.varidiansms.services.SmsProcessor;

import java.util.concurrent.Executors;

/**
 * Delivery report from the recipient handset -> message.phone.delivered.
 * Note: delivery reports depend on carrier support.
 */
public class SmsDeliveredReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String messageId = intent.getStringExtra(SmsProcessor.EXTRA_MESSAGE_ID);
        if (messageId == null) return;

        final int code = getResultCode();
        final PendingResult pending = goAsync();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (code == Activity.RESULT_OK) {
                    new ApiClient(context).reportMessageEvent(messageId, "DELIVERED", null);
                }
            } finally {
                pending.finish();
            }
        });
    }
}
