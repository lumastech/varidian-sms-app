package com.varidian.varidiansms.api;

import android.content.Context;
import android.util.Log;

import com.varidian.varidiansms.util.AppPrefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Blocking HTTP client for the Laravel device API (/api/v1/phone/*).
 * Call only from background threads / workers — never the main thread.
 */
public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private final AppPrefs prefs;

    public ApiClient(Context context) {
        this.prefs = new AppPrefs(context);
    }

    /** POST /api/v1/phone/register */
    public boolean register(String fcmToken) {
        try {
            JSONObject body = new JSONObject()
                    .put("phone_number", prefs.getPhoneNumber())
                    .put("fcm_token", fcmToken == null ? JSONObject.NULL : fcmToken);
            return post("/api/v1/phone/register", body) != null;
        } catch (Exception e) {
            Log.e(TAG, "register failed", e);
            return false;
        }
    }

    /** POST /api/v1/phone/heartbeat */
    public boolean heartbeat() {
        try {
            JSONObject body = new JSONObject().put("phone_number", prefs.getPhoneNumber());
            return post("/api/v1/phone/heartbeat", body) != null;
        } catch (Exception e) {
            Log.e(TAG, "heartbeat failed", e);
            return false;
        }
    }

    /** GET /api/v1/phone/messages/pending — returns the "data" array or null. */
    public JSONArray fetchPendingMessages() {
        try {
            Request request = new Request.Builder()
                    .url(prefs.getServerUrl() + "/api/v1/phone/messages/pending?phone_number="
                            + java.net.URLEncoder.encode(prefs.getPhoneNumber(), "UTF-8"))
                    .header("x-api-key", prefs.getApiKey())
                    .header("Accept", "application/json")
                    .get()
                    .build();

            try (Response response = http.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return null;
                JSONObject json = new JSONObject(response.body().string());
                return json.optJSONArray("data");
            }
        } catch (Exception e) {
            Log.e(TAG, "fetchPendingMessages failed", e);
            return null;
        }
    }

    /** POST /api/v1/phone/messages/{id}/events — event: SENT | DELIVERED | FAILED */
    public boolean reportMessageEvent(String messageId, String event, String reason) {
        try {
            JSONObject body = new JSONObject()
                    .put("event", event)
                    .put("phone_number", prefs.getPhoneNumber());
            if (reason != null) body.put("reason", reason);
            return post("/api/v1/phone/messages/" + messageId + "/events", body) != null;
        } catch (Exception e) {
            Log.e(TAG, "reportMessageEvent failed", e);
            return false;
        }
    }

    /** POST /api/v1/phone/messages/receive — forward an incoming SMS. */
    public boolean forwardIncomingSms(String from, String content, String sim) {
        try {
            JSONObject body = new JSONObject()
                    .put("from", from)
                    .put("content", content)
                    .put("sim", sim)
                    .put("phone_number", prefs.getPhoneNumber());
            return post("/api/v1/phone/messages/receive", body) != null;
        } catch (Exception e) {
            Log.e(TAG, "forwardIncomingSms failed", e);
            return false;
        }
    }

    /** POST /api/v1/phone/calls/missed */
    public boolean reportMissedCall(String from) {
        try {
            JSONObject body = new JSONObject()
                    .put("from", from)
                    .put("phone_number", prefs.getPhoneNumber());
            return post("/api/v1/phone/calls/missed", body) != null;
        } catch (Exception e) {
            Log.e(TAG, "reportMissedCall failed", e);
            return false;
        }
    }

    private JSONObject post(String path, JSONObject body) throws IOException {
        Request request = new Request.Builder()
                .url(prefs.getServerUrl() + path)
                .header("x-api-key", prefs.getApiKey())
                .header("Accept", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = http.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                Log.w(TAG, "POST " + path + " -> " + response.code() + " " + responseBody);
                return null;
            }
            try {
                return new JSONObject(responseBody);
            } catch (Exception e) {
                return new JSONObject();
            }
        }
    }
}
