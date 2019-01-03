package com.vodafone.lib.seclibng.internal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

import com.vodafone.lib.seclibng.Event;
import com.vodafone.lib.seclibng.comms.Config;
import com.vodafone.lib.seclibng.comms.Logger;
import com.vodafone.lib.seclibng.comms.Protocol;
import com.vodafone.lib.seclibng.comms.SharedPref;
import com.vodafone.lib.seclibng.encryption.KeytoolHelper;
import com.vodafone.lib.seclibng.storage.SqliteDb;

/**
 * EventService class responsible for sending events to backend.
 */
public class EventsIntentService extends JobIntentService implements AsyncResponse{

    private static final int UNIQUE_JOB_ID=1000;

    private static final String eventServiceTag = "EventsIntentService";
    public AsyncResponse delegate=null;

    @Override
    public void processFinish(boolean output) {

        SharedPref.setConfigKeys(getApplicationContext(),Config.KEYNAME_SEND_EVENT_SERVICE_RUNNING_STATUS,"finished");
    }

    public static void enqueueWork(Context context, Intent i) {
        SharedPref.setConfigKeys(context,Config.KEYNAME_SEND_EVENT_SERVICE_RUNNING_STATUS,"running");
        enqueueWork(context, EventsIntentService.class, UNIQUE_JOB_ID, i);
    }

    @SuppressLint("LongLogTag")
    @Override
    protected void onHandleWork(@NonNull Intent intent) {

        String intentAction;
        SqliteDb db;
        boolean isNoUserIdEvent = false;
        SharedPref.setConfigKeys(getApplicationContext(),Config.KEYNAME_SEND_EVENT_SERVICE_RUNNING_STATUS,"running");
        SharedPref.setConfigKeys(getApplicationContext(), Config.KEYNAME_ALARM_FLAG, Config.KEYNAME_ALARM_FLAG_RESET);
        if (!Config.isOnline(this) || SharedPref.getResetCountStatusBlockEvent(this)) {
            if (SharedPref.getResetCountStatusBlockEvent(this)) {
                Logger.i(eventServiceTag, "Device has reached maximum reset count");
            } else {
                Logger.i(eventServiceTag, "Device is offline. Service is aborting");
            }
            stopSelf();
        } else if (SharedPref.getConfigKeys(getApplicationContext(), Config.KEYNAME_HOME_DOC_RETRIEVED, Config.KEYNAME_HOME_DOC_RETRIEVED_NO).equalsIgnoreCase(Config.KEYNAME_HOME_DOC_RETRIEVED_NO)) {
            Logger.i(eventServiceTag, "Home doc not yet received");
            stopSelf();
        } else if (KeytoolHelper.getDecKey() == null) {
            Logger.i(eventServiceTag, "Application not running");
            stopSelf();
        } else {

            intentAction = intent.getStringExtra("action");

            if(intentAction != null)
              isNoUserIdEvent = intentAction.equalsIgnoreCase(Config.NO_USER_ID_DATA);

            int currentCount = SharedPref.getCurrentEventsSentCount(this);
            int maxCount = Integer.parseInt(SharedPref.getConfigKeys(this, Config.KEYNAME_MAX_NO_OF_EVENTS, Config.DEFAULT_MAX_NO_OF_EVENTS));

            if (currentCount < maxCount) {
                db = SqliteDb.getInstance(EventsIntentService.this);
                String submitEventUrl = SharedPref.getConfigKeys(getApplicationContext(), Config.KEYNAME_SUBMIT_EVENTS, "");
                if (db.hasMoreEvents() && !submitEventUrl.isEmpty()) {
                    Event[] events = isNoUserIdEvent ? db.getEventDataArrayAllNoUserIdEvents() : db.getEventDataArrayAll();
                    Protocol mProtocol = isNoUserIdEvent ? new Protocol(EventsIntentService.this, true, db.getNouserIdcurrentTime()) : new Protocol(EventsIntentService.this, false, 0);

                    if (events.length > 0) {
                        AsyncPushEvents asyncTask = new AsyncPushEvents(getApplicationContext(), mProtocol);
                        asyncTask.delegate = this;
                        asyncTask.execute(events);
                    }
                } else {
                    Logger.i(eventServiceTag, "Database is empty. Service is aborting");
                    stopSelf();
                }
            } else {
                Logger.i(eventServiceTag, "Device has reached maximum no of events that can sent in one reset period. Aborting service");
                stopSelf();
            }
        }
    }
}
