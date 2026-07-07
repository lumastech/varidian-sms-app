package com.varidian.varidiansms.receivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

import com.varidian.varidiansms.api.ApiClient;
import com.varidian.varidiansms.services.SmsProcessor;

import java.util.concurrent.Executors;

/**
 * Radio callback: the SMS left the device (or failed). Reports
 * SENT / FAILED back to the Laravel backend, which then emits the
 * message.phone.sent or message.send.failed webhook.
 */
public class SmsSentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String messageId = intent.getStringExtra(SmsProcessor.EXTRA_MESSAGE_ID);
        if (messageId == null) return;

        final int code = getResultCode();
        final PendingResult pending = goAsync();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ApiClient api = new ApiClient(context);
                if (code == Activity.RESULT_OK) {
                    api.reportMessageEvent(messageId, "SENT", null);
                } else {
                    api.reportMessageEvent(messageId, "FAILED", errorName(code));
                }
            } finally {
                pending.finish();
            }
        });
    }

    private static String errorName(int code) {
        switch (code) {
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE: return "GENERIC_FAILURE";
            case SmsManager.RESULT_ERROR_NO_SERVICE:      return "NO_SERVICE";
            case SmsManager.RESULT_ERROR_NULL_PDU:        return "NULL_PDU";
            case SmsManager.RESULT_ERROR_RADIO_OFF:       return "RADIO_OFF";
            case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED:  return "LIMIT_EXCEEDED";
            default:                                      return "ERROR_CODE_" + code;
        }
    }
}
