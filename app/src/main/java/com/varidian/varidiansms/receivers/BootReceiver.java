package com.varidian.varidiansms.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.varidian.varidiansms.services.PollingForegroundService;
import com.varidian.varidiansms.util.AppPrefs;
import com.varidian.varidiansms.workers.HeartbeatWorker;

/** Restarts the gateway automatically after a device reboot. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        AppPrefs prefs = new AppPrefs(context);
        if (!prefs.isLoggedIn() || !prefs.isGatewayEnabled()) return;

        HeartbeatWorker.schedule(context);
        PollingForegroundService.start(context);
    }
}
