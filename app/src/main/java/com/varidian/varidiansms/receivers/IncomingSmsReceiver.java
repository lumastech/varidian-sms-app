package com.varidian.varidiansms.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import com.varidian.varidiansms.api.ApiClient;
import com.varidian.varidiansms.util.AppPrefs;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * SMS arriving on this phone are forwarded to the backend, which stores
 * them as mobile-originated messages and fires the
 * message.phone.received webhook. Forwarding is opt-in: it only runs
 * when the user enables it in the gateway settings.
 *
 * Multi-part messages arrive as several PDUs from the same sender —
 * we stitch them back together before forwarding.
 */
public class IncomingSmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        AppPrefs prefs = new AppPrefs(context);
        if (!prefs.isLoggedIn() || !prefs.isForwardIncomingEnabled()) return;

        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts == null || parts.length == 0) return;

        // Reassemble multipart bodies keyed by sender.
        Map<String, StringBuilder> bodies = new HashMap<>();
        for (SmsMessage part : parts) {
            if (part == null) continue;
            String from = part.getDisplayOriginatingAddress();
            bodies.computeIfAbsent(from, k -> new StringBuilder())
                  .append(part.getMessageBody());
        }

        // Best-effort SIM detection from the broadcast extras.
        int slot = intent.getIntExtra("slot", intent.getIntExtra("android.telephony.extra.SLOT_INDEX", -1));
        final String sim = slot == 0 ? "SIM1" : slot == 1 ? "SIM2" : "DEFAULT";

        final PendingResult pending = goAsync();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ApiClient api = new ApiClient(context);
                for (Map.Entry<String, StringBuilder> entry : bodies.entrySet()) {
                    api.forwardIncomingSms(entry.getKey(), entry.getValue().toString(), sim);
                }
            } finally {
                pending.finish();
            }
        });
    }
}
