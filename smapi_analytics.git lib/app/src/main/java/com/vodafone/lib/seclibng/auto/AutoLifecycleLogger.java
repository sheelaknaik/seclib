package com.vodafone.lib.seclibng.auto;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import com.vodafone.lib.seclibng.Event;
import com.vodafone.lib.seclibng.SecLibNG;
import com.vodafone.lib.seclibng.comms.Config;
import com.vodafone.lib.seclibng.comms.EventConstants;
import com.vodafone.lib.seclibng.comms.Logger;
import com.vodafone.lib.seclibng.comms.SharedPref;

import org.json.JSONException;

import java.util.List;
import java.util.UUID;

/**
 * This class is responsible for logging the life cycle events.
 */

public class AutoLifecycleLogger implements Application.ActivityLifecycleCallbacks {
    private final String tagLifecycleLogger = "AutoLifecycleLogger";
    private static String sSessionId;
    private final boolean mLogCreatedDestroyed = true;
    private final boolean mLogStartedStopped = true;
    private final boolean mLogResumedPaused = true;
    private static Application mApp;
    private int mSessionDepth;
    private long onCreateTime = 0;
    private static String currentActivity;
    private boolean orientationFlag = false;
    private static boolean isAppInBackground = true;

    /***
     * enum for activity Lifecycle events
     */
    public enum ActivityLifecycle {
        CREATED,
        STARTED,
        RESUMED,
        PAUSED,
        STOPPED,
        DESTROYED
    }

    /***
     * Constructor to initialize the Auto lifecycle related event.
     *
     * @param app application object
     */

    public AutoLifecycleLogger(Application app) {
        mApp = app;
        mSessionDepth = 0;
    }

    /***
     * This method is used to get the session ID
     *
     * @return returns the session ID
     */
    public static String getsSessionId() {
        return sSessionId;
    }

    /***
     * Method use to set the session ID
     *
     * @param sSessionId session id to set
     */
    private static void setsSessionId(String sSessionId) {
        AutoLifecycleLogger.sSessionId = sSessionId;
        if (AutoLifecycleLogger.sSessionId != null) {
            Context context = mApp;
            SharedPref.setConfigKeys(context, Config.KEYNAME_LAST_SESSION, sSessionId);
        }
    }

    /***
     * Register for the activity lifecycle call back events
     */

    public void registerActivityLifecycleCallbacks() {
        if (mApp == null) {
            return;
        }

        mApp.registerActivityLifecycleCallbacks(this);
    }

    /***
     * Stop listening for activity life cycle events
     */
    public void unregisterActivityLifecycleCallbacks() {
        if (mApp == null) {
            return;
        }
        mApp.unregisterActivityLifecycleCallbacks(this);
    }



    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (!mLogCreatedDestroyed) {
            return;
        }

        if (savedInstanceState != null) {
            sSessionId = savedInstanceState.getString("sessionID");
        }

        try {
            setCurrentActivity(activity.getLocalClassName().replace(".java", ""));
            mSessionDepth++;
            if (mSessionDepth == 1 && sSessionId == null) {
                setsSessionId(UUID.randomUUID().toString());
            }

            onCreateTime = System.currentTimeMillis();
            SecLibNG.getInstance().logEvent(createEvent(activity, ActivityLifecycle.CREATED.name()));
        } catch (JSONException e) {
            Logger.e(tagLifecycleLogger, "Error while creating event for onActivityCreated");
        }
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onActivityStarted(Activity activity) {

        if (!mLogStartedStopped) {
            return;
        }

        try {
            SecLibNG.getInstance().logEvent(createEvent(activity, ActivityLifecycle.STARTED.name()));
            isAppIsInBackground(mApp, activity);
        } catch (JSONException e) {
            Logger.e(tagLifecycleLogger, "Error while creating event for OnActivityStarted");
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        setCurrentActivity(activity.getLocalClassName());
        if (!mLogResumedPaused) {
            return;
        }
        if (onCreateTime > 0) {
            long currentTime = System.currentTimeMillis();
            long timeDifference = currentTime - onCreateTime;

            onCreateTime = 0;
            try {
                SecLibNG.getInstance().logEvent(createEventForPageLoadTime(activity, Double.toString(timeDifference / 1000.0)));
            } catch (JSONException je) {
                Logger.e(tagLifecycleLogger, "Error while creating event for pageLoadTime");
            }
        }
        try {
            SecLibNG.getInstance().logEvent(createEvent(activity, ActivityLifecycle.RESUMED.name()));
        } catch (JSONException e) {
            Logger.e(tagLifecycleLogger, "Error while creating event for OnActivityResumed");
        }
    }


    @Override
    public void onActivityPaused(Activity activity) {
        if (!mLogResumedPaused) {
            return;
        }
        try {
            SecLibNG.getInstance().logEvent(createEvent(activity, ActivityLifecycle.PAUSED.name()));
        } catch (JSONException e) {
            Logger.e(tagLifecycleLogger, "Error while creating event for onActivityPaused");
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (!mLogStartedStopped) {
            return;
        }
        try {

            SecLibNG.getInstance().logEvent(createEvent(activity, ActivityLifecycle.STOPPED.name()));
            isAppIsInBackground(mApp, activity);
        } catch (JSONException e) {
            Logger.e(tagLifecycleLogger, "Error while creating event for OnActivityStopped", e);
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        outState.putString("sessionID", AutoLifecycleLogger.sSessionId);
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (!mLogCreatedDestroyed) {
            return;
        }
        if (mSessionDepth > 0) {
            mSessionDepth--;
        }
        try {
            SecLibNG.getInstance().logEvent(createEvent(activity, ActivityLifecycle.DESTROYED.name()));
        } catch (JSONException e) {
            Logger.e(tagLifecycleLogger, "Error while creating event for onActivityDestroyed", e);
        }
    }

    /***
     * Creates event for activity life cycle.
     *
     * @param activity activity name
     * @return returns the event object
     * @throws JSONException throws exception
     */
    private Event createEvent(Activity activity, String lifecycleName) throws JSONException {
        try {
            Context context = mApp.getApplicationContext();
            Event event = new Event(Event.EventType.PAGE, context);
            event.addPayload(EventConstants.EVENT_DESCRIPTION, lifecycleName);
            event.addPayload(EventConstants.EVENT_ELEMENT, Config.DEFAULT_NA);
            event.addPayload(EventConstants.X_VF_PAGE, activity.getLocalClassName());
            event.addPayload(EventConstants.X_VF_SUB_PAGE, Config.DEFAULT_NA);
            if (lifecycleName.equals(ActivityLifecycle.DESTROYED.name())) {
                if (mSessionDepth == 0) {
                    setsSessionId(null);
                }
            }
            return event;
        } catch (Exception e) {
            Logger.e(tagLifecycleLogger, "Unable to create an event "+e.getMessage(), e);
        }
        return null;
    }

    /***
     * Creates event for application in foreground/background
     *
     * @param activity activity name
     * @return returns the event object
     * @throws JSONException throws exception
     */
    private Event createApplicationEvent(Activity activity, String description) throws JSONException {
        try {
            Context context = mApp.getApplicationContext();
            Event event = new Event(Event.EventType.APPLICATION, context);
            event.addPayload(EventConstants.EVENT_DESCRIPTION, description);
            event.addPayload(EventConstants.EVENT_ELEMENT, Config.DEFAULT_NA);
            event.addPayload(EventConstants.X_VF_PAGE, activity.getLocalClassName());
            event.addPayload(EventConstants.X_VF_SUB_PAGE, Config.DEFAULT_NA);

            return event;
        } catch (Exception e) {
            Logger.e(tagLifecycleLogger, "Unable to create an event "+e.getMessage(), e);
        }
        return null;
    }

    /***
     * Creates event for page load time
     *
     * @param activity     activity name
     * @param pageLoadTime Page load time
     * @return returns the event object
     * @throws JSONException throws exception
     */
    private Event createEventForPageLoadTime(Activity activity, String pageLoadTime) throws JSONException {
        try {
            Context context = mApp.getApplicationContext();
            Event event = new Event(Event.EventType.PAGE, context);
            event.addPayload(EventConstants.EVENT_DESCRIPTION, pageLoadTime + " Sec");
            event.addPayload(EventConstants.EVENT_ELEMENT, Config.PAGE_LOAD_TIME);
            event.addPayload(EventConstants.X_VF_PAGE, activity.getLocalClassName());
            event.addPayload(EventConstants.X_VF_SUB_PAGE, Config.DEFAULT_NA);
            return event;
        } catch (Exception e) {
            Logger.e(tagLifecycleLogger, "Unable to create an EVENT "+e.getMessage(), e);
        }
        return null;
    }

    /***
     * Get Current Activity name
     *
     * @return Returns current activity name
     */
    public static String getCurrentActivity() {
        return  currentActivity==null?Config.DEFAULT_NA:currentActivity;
    }

    /***
     * Sets current activity name
     *
     * @param currentActivity Current activity name
     */
    private static void setCurrentActivity(String currentActivity) {
        AutoLifecycleLogger.currentActivity = currentActivity;
    }


    private void isAppIsInBackground(Context context, Activity activity) throws JSONException {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            if(runningProcesses != null) {
                for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                    if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        for (String activeProcess : processInfo.pkgList) {
                            if (activeProcess.equals(context.getPackageName())) {
                                isInBackground = false;
                            }
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            if(taskInfo != null) {
                ComponentName componentInfo = taskInfo.get(0).topActivity;
                if (componentInfo.getPackageName().equals(context.getPackageName())) {
                    isInBackground = false;
                }
            }
        }
        if(isAppInBackground != isInBackground) {
            isAppInBackground = isInBackground;
            SecLibNG.getInstance().logEvent(createApplicationEvent(activity, isAppInBackground ? Config.APP_BACKGROUND : Config.APP_FOREGROUND));
        }
    }
}
