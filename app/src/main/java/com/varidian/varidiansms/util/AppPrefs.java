package com.varidian.varidiansms.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persisted config, split in two halves:
 *
 *  ACCOUNT — master API key (vsk_…) and profile obtained by logging in
 *  with email/password. Used by the portal screens (dashboard, messages,
 *  phones, webhooks, keys). The server URL is fixed ({@link #SERVER_URL}).
 *
 *  GATEWAY — this phone's number and an optional dedicated phone API key
 *  (vpk_…). Used by the background services that turn the device into an
 *  SMS gateway. When no dedicated key is set, the account key is used.
 */
public class AppPrefs {
    /** Fixed backend the app always talks to. */
    public static final String SERVER_URL = "https://sms.varidianlab.com";

    private static final String FILE = "gateway_prefs";
    private final SharedPreferences prefs;

    public AppPrefs(Context context) {
        prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    // ---------------------------------------------------------------- account

    public String getServerUrl()      { return SERVER_URL; }
    public String getAccountApiKey()  { return prefs.getString("account_api_key", ""); }
    public String getUserName()       { return prefs.getString("user_name", ""); }
    public String getUserEmail()      { return prefs.getString("user_email", ""); }

    public boolean isAccountLoggedIn() {
        return !getAccountApiKey().isEmpty();
    }

    public void saveAccount(String accountApiKey, String userName, String userEmail) {
        prefs.edit()
             .putString("account_api_key", accountApiKey.trim())
             .putString("user_name", userName)
             .putString("user_email", userEmail)
             .apply();
    }

    /** Log out: wipes the account AND stops gateway credentials from working. */
    public void clearAccount() {
        prefs.edit()
             .remove("account_api_key")
             .remove("user_name")
             .remove("user_email")
             .apply();
    }

    // ---------------------------------------------------------------- gateway

    /** Dedicated phone API key (vpk_…). Empty means "use the account key". */
    public String getGatewayApiKey() { return prefs.getString("api_key", ""); }

    public String getPhoneNumber()   { return prefs.getString("phone_number", ""); }

    /**
     * Key used by the gateway services in the x-api-key header:
     * the dedicated phone key when set, otherwise the account key.
     */
    public String getApiKey() {
        String dedicated = getGatewayApiKey();
        return dedicated.isEmpty() ? getAccountApiKey() : dedicated;
    }

    /** True when the background gateway has everything it needs to run. */
    public boolean isLoggedIn() {
        return !getApiKey().isEmpty() && !getPhoneNumber().isEmpty();
    }

    public boolean isGatewayEnabled() { return prefs.getBoolean("gateway_enabled", false); }

    public void setGatewayEnabled(boolean enabled) {
        prefs.edit().putBoolean("gateway_enabled", enabled).apply();
    }

    /** Opt-in: forward SMS received on this phone to the server. Off by default. */
    public boolean isForwardIncomingEnabled() { return prefs.getBoolean("forward_incoming_enabled", false); }

    public void setForwardIncomingEnabled(boolean enabled) {
        prefs.edit().putBoolean("forward_incoming_enabled", enabled).apply();
    }

    /** Opt-in: report missed calls on this phone to the server. Off by default. */
    public boolean isReportMissedCallsEnabled() { return prefs.getBoolean("report_missed_calls_enabled", false); }

    public void setReportMissedCallsEnabled(boolean enabled) {
        prefs.edit().putBoolean("report_missed_calls_enabled", enabled).apply();
    }

    public void saveGateway(String phoneApiKey, String phoneNumber) {
        prefs.edit()
             .putString("api_key", phoneApiKey.trim())
             .putString("phone_number", phoneNumber.trim())
             .apply();
    }

    public void clear() { prefs.edit().clear().apply(); }
}
