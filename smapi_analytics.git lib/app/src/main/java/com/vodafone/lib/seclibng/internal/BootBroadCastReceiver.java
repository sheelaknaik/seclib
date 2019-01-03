package com.vodafone.lib.seclibng.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.vodafone.lib.seclibng.comms.Config;
import com.vodafone.lib.seclibng.comms.EntryPointHandling;
import com.vodafone.lib.seclibng.comms.Logger;
import com.vodafone.lib.seclibng.comms.SharedPref;

/**
 * Broadcast receiver class which inform the app for each network change detected and boot completed.
 */
public class BootBroadCastReceiver extends BroadcastReceiver {

    private static final String bootReceiverTag = "BootBroadCastReceiver.";

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onReceive(Context context, Intent intent) {

        if (!isInitialStickyBroadcast()) {
            boolean isHomeDocYetToload = false;

            if(Intent.ACTION_BOOT_COMPLETED.equalsIgnoreCase(intent.getAction())){
                Logger.i(bootReceiverTag, "Reset Alarm flag");
                SharedPref.setConfigKeys(context, Config.KEYNAME_ALARM_FLAG, Config.KEYNAME_ALARM_FLAG_RESET);
            } else {
                if (Config.isOnline(context)) {
                    if (SharedPref.getConfigKeys(context, Config.KEYNAME_HOME_DOC_RETRIEVED, Config.KEYNAME_HOME_DOC_RETRIEVED_NO).equalsIgnoreCase(Config.KEYNAME_HOME_DOC_RETRIEVED_NO)) {
                        Intent intentHomeDoc = new Intent(context, EntryPointHandling.class).putExtra("action", Config.HOME_DOC);
                        EntryPointHandling.enqueueWork(context, intentHomeDoc);
                        isHomeDocYetToload = true;
                    }
                    if (SharedPref.getConfigKeys(context, Config.KEYNAME_SETTINGS_FLAG, Config.KEYNAME_SETTINGS_SET).equalsIgnoreCase(Config.KEYNAME_SETTINGS_SET)) {
                        if (!isHomeDocYetToload){
                            Intent intentSettings = new Intent(context, EntryPointHandling.class).putExtra("action", Config.SETTINGS);
                            EntryPointHandling.enqueueWork(context, intentSettings);
                    }
                  }
                } else {
                    Logger.i(bootReceiverTag, "Device is offline");
                }
            }
        }
    }
}
