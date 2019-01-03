package com.vodafone.lib.seclibng.comms;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.vodafone.lib.seclibng.Event;
import com.vodafone.lib.seclibng.encryption.KeytoolHelper;
import com.vodafone.lib.seclibng.internal.AsyncPushEvents;
import com.vodafone.lib.seclibng.internal.AsyncResponse;
import com.vodafone.lib.seclibng.storage.SqliteDb;

/***
 * JobService class for job scheduler
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class JobServiceForEvent extends JobService implements AsyncResponse {
    private String SMAPIStatus;
    private static final String eventServiceTag = "JobServiceForEvent";

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        try {
            SqliteDb db;
            boolean isNoUserIdEvent;

            SMAPIStatus = SharedPref.getConfigKeys(getApplicationContext(), Config.KEYNAME_SMAPI_STATUS, "ON");

            if (SMAPIStatus.equalsIgnoreCase("ON")) {

                SharedPref.setConfigKeys(getApplicationContext(), Config.KEYNAME_SEND_EVENT_SERVICE_RUNNING_STATUS, "running");
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
                    isNoUserIdEvent = false;
                    int currentCount = SharedPref.getCurrentEventsSentCount(this);
                    int maxCount = Integer.parseInt(SharedPref.getConfigKeys(this, Config.KEYNAME_MAX_NO_OF_EVENTS, Config.DEFAULT_MAX_NO_OF_EVENTS));
                    if (currentCount < maxCount) {
                        db = SqliteDb.getInstance(this);
                        String submitEventUrl = SharedPref.getConfigKeys(getApplicationContext(), Config.KEYNAME_SUBMIT_EVENTS, "");
                        if (db.hasMoreEvents() && !submitEventUrl.isEmpty()) {
                            Event[] events = isNoUserIdEvent ? db.getEventDataArrayAllNoUserIdEvents() : db.getEventDataArrayAll();
                            Protocol mProtocol = isNoUserIdEvent ? new Protocol(this, true, db.getNouserIdcurrentTime()) : new Protocol(this, false, 0);
                            if (events.length > 0) {
                                AsyncPushEvents asyncTask = new AsyncPushEvents(getApplicationContext(), mProtocol);
                                asyncTask.delegate = this;
                                asyncTask.execute(events);
                            }
                        } else {
                            Logger.i(eventServiceTag, "Empty Database. Service is aborting");
                            stopSelf();
                        }
                    } else {
                        Logger.i(eventServiceTag, "Device has reached maximum no of events that can send in one reset period. Aborting service");
                        stopSelf();
                    }
                }
            }
        } catch (IllegalStateException e) {
            SharedPref.setConfigKeys(getApplicationContext(), Config.KEYNAME_ALARM_FLAG, Config.KEYNAME_ALARM_FLAG_RESET);
            Logger.e("JobService", "Unable to start service :" + e.getMessage());
        } catch (Exception e) {
            Logger.e("JobService", "Exception while sending events to the server :" + e.getMessage());
        }

        jobFinished(jobParameters, false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (SMAPIStatus != null && SMAPIStatus.equalsIgnoreCase("ON") && getApplicationContext() != null) {
            SharedPref.setConfigKeys(getApplicationContext(), Config.KEYNAME_SEND_EVENT_SERVICE_RUNNING_STATUS, "finished");
        }
    }

    @Override
    public void processFinish(boolean output) {
        stopSelf();
        if (SMAPIStatus != null && SMAPIStatus.equalsIgnoreCase("ON") && getApplicationContext() != null) {
            SharedPref.setConfigKeys(getApplicationContext(), Config.KEYNAME_SEND_EVENT_SERVICE_RUNNING_STATUS, "finished");
        }
    }
}
