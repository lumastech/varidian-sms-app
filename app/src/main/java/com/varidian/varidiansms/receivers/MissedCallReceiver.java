package com.varidian.varidiansms.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.varidian.varidiansms.api.ApiClient;
import com.varidian.varidiansms.util.AppPrefs;

import java.util.concurrent.Executors;

/**
 * Missed call = RINGING -> IDLE with no OFFHOOK in between.
 * Fires the message.call.missed webhook via the backend.
 * Reporting is opt-in: it only runs when the user enables it in the
 * gateway settings.
 */
public class MissedCallReceiver extends BroadcastReceiver {

    private static String lastState = TelephonyManager.EXTRA_STATE_IDLE;
    private static String ringingNumber = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) return;
        AppPrefs prefs = new AppPrefs(context);
        if (!prefs.isLoggedIn() || !prefs.isReportMissedCallsEnabled()) return;

        String state  = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
        if (state == null) return;

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            ringingNumber = number;
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)
                && TelephonyManager.EXTRA_STATE_RINGING.equals(lastState)
                && ringingNumber != null) {

            final String from = ringingNumber;
            ringingNumber = null;

            final PendingResult pending = goAsync();
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    new ApiClient(context).reportMissedCall(from);
                } finally {
                    pending.finish();
                }
            });
        }

        lastState = state;
    }
}
