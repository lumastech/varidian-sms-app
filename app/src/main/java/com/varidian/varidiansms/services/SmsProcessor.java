package com.varidian.varidiansms.services;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.varidian.varidiansms.api.ApiClient;
import com.varidian.varidiansms.receivers.SmsDeliveredReceiver;
import com.varidian.varidiansms.receivers.SmsSentReceiver;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Pulls queued messages from the server and hands them to the Android
 * telephony stack. Server-side rate limiting (messages_per_minute) already
 * spaced the messages out, so everything the pending endpoint returns is
 * due NOW.
 *
 * For every SMS we register two PendingIntents:
 *   - SENT      -> SmsSentReceiver      -> POST .../events {event: SENT|FAILED}
 *   - DELIVERED -> SmsDeliveredReceiver -> POST .../events {event: DELIVERED}
 */
public class SmsProcessor {
    private static final String TAG = "SmsProcessor";

    public static final String ACTION_SMS_SENT      = "com.varidian.varidiansms.SMS_SENT";
    public static final String ACTION_SMS_DELIVERED = "com.varidian.varidiansms.SMS_DELIVERED";
    public static final String EXTRA_MESSAGE_ID     = "message_id";

    /** Fetch everything due and send it. Call from a background thread. */
    public static void processPending(Context context) {
        ApiClient api = new ApiClient(context);
        JSONArray pending = api.fetchPendingMessages();
        if (pending == null || pending.length() == 0) return;

        Log.i(TAG, "Processing " + pending.length() + " pending message(s)");

        for (int i = 0; i < pending.length(); i++) {
            JSONObject message = pending.optJSONObject(i);
            if (message == null) continue;

            String id      = message.optString("id");
            String to      = message.optString("contact");
            String content = message.optString("content");
            String sim     = message.optString("sim", "DEFAULT");

            try {
                sendSms(context, id, to, content, sim);
            } catch (Exception e) {
                Log.e(TAG, "Send failed for " + id, e);
                api.reportMessageEvent(id, "FAILED", e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    @SuppressLint("MissingPermission")
    private static void sendSms(Context context, String messageId, String to, String content, String sim) {
        SmsManager smsManager = resolveSmsManager(context, sim);

        ArrayList<String> parts = smsManager.divideMessage(content);

        ArrayList<PendingIntent> sentIntents      = new ArrayList<>();
        ArrayList<PendingIntent> deliveredIntents = new ArrayList<>();

        for (int part = 0; part < parts.size(); part++) {
            // Only attach the callback to the LAST part so multi-part
            // messages report exactly one SENT / DELIVERED event.
            boolean last = part == parts.size() - 1;

            sentIntents.add(last
                    ? pendingIntent(context, SmsSentReceiver.class, ACTION_SMS_SENT, messageId)
                    : null);
            deliveredIntents.add(last
                    ? pendingIntent(context, SmsDeliveredReceiver.class, ACTION_SMS_DELIVERED, messageId)
                    : null);
        }

        smsManager.sendMultipartTextMessage(to, null, parts, sentIntents, deliveredIntents);
        Log.i(TAG, "Dispatched SMS " + messageId + " to " + to + " (" + parts.size() + " part(s), " + sim + ")");
    }

    /** Map SIM1/SIM2/DEFAULT to the right SmsManager on dual-SIM devices. */
    @SuppressLint("MissingPermission")
    private static SmsManager resolveSmsManager(Context context, String sim) {
        if (!"SIM1".equals(sim) && !"SIM2".equals(sim)) {
            return SmsManager.getDefault();
        }

        int wantedSlot = "SIM1".equals(sim) ? 0 : 1;

        try {
            SubscriptionManager subs = (SubscriptionManager)
                    context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (subs != null && subs.getActiveSubscriptionInfoList() != null) {
                for (SubscriptionInfo info : subs.getActiveSubscriptionInfoList()) {
                    if (info.getSimSlotIndex() == wantedSlot) {
                        return SmsManager.getSmsManagerForSubscriptionId(info.getSubscriptionId());
                    }
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "READ_PHONE_STATE not granted; falling back to default SIM");
        }

        return SmsManager.getDefault();
    }

    private static PendingIntent pendingIntent(Context context, Class<?> receiver, String action, String messageId) {
        Intent intent = new Intent(context, receiver)
                .setAction(action)
                .putExtra(EXTRA_MESSAGE_ID, messageId);

        // Unique requestCode per message so extras aren't collapsed.
        return PendingIntent.getBroadcast(
                context,
                messageId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
