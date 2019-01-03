package com.vodafone.lib.seclibng.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.vodafone.lib.seclibng.comms.Logger;

/**
 * Responsible for calling the JobIntentService for alarm
 */

public class AlarmReceiver extends BroadcastReceiver {

    private static final String eventReceiverTag = "AlarmReceiver.";

    @Override
    public void onReceive(Context context, Intent intent) {

        Logger.i(eventReceiverTag, "calling EventsIntentService Service");

        EventsIntentService.enqueueWork(context, intent);
    }
}