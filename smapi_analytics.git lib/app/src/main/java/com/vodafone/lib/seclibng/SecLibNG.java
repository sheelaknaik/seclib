package com.vodafone.lib.seclibng;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;

import com.vodafone.lib.seclibng.auto.AutoLifecycleLogger;
import com.vodafone.lib.seclibng.auto.AutoLogManager;
import com.vodafone.lib.seclibng.comms.Config;
import com.vodafone.lib.seclibng.comms.CrashHandler;
import com.vodafone.lib.seclibng.comms.EntryPointHandling;
import com.vodafone.lib.seclibng.comms.EventConstants;
import com.vodafone.lib.seclibng.comms.Logger;
import com.vodafone.lib.seclibng.comms.SharedPref;
import com.vodafone.lib.seclibng.encryption.KeytoolHelper;
import com.vodafone.lib.seclibng.internal.CreateKeyStoreAsync;
import com.vodafone.lib.seclibng.internal.DecryptionAsync;
import com.vodafone.lib.seclibng.internal.EventsIntentService;
import com.vodafone.lib.seclibng.network.OkHttpCallRepo;
import com.vodafone.lib.seclibng.storage.SqliteDb;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.UUID;


/**
 * Main Entry point for the SecLib which initialize the library with the main application.
 */
public class SecLibNG {
    private static final String TAGSECLIB = "SecLibNG";
    private static SecLibNG sSecLib;
    public static final String ENVIRONMENT_PRE = "environment_pre";
    public static final String ENVIRONMENT_PRO = "environment_pro";
    private static String environmentString = ENVIRONMENT_PRE;
    private AutoLogManager mAutoLogManager;
    private Context mContext;
    private final String TAG_SECLIBNG = "SecLibNG.logEvent";
    private final String missingDb = "Missing Sqlite DB";
    private static boolean verboseStatus = false;
    private SqliteDb db;
    private String uUid;
    private SharedPreferences sharedPreferences;

    /***
     * Private Constructor
     */
    private SecLibNG() {
    }

    /***
     * Set user Id setUserId
     *
     * @param sUserId User ID that has o set for the secLib
     * @param context Context Object
     */
    public void setsUserId(String sUserId, Context context) {
        String currentUserId = SharedPref.getConfigKeys(context, Config.KEYNAME_USER_ID, Config.KEYNAME_USER_ID_UNKNOWN);
        if (!currentUserId.equalsIgnoreCase(sUserId)) {
            //TODO Updates the no of resets received
            // Config.updateResetCountReceived(context);
            SharedPref.setConfigKeys(context, Config.KEYNAME_USER_ID, sUserId);
        }
    }

    /***
     * Set trace id in Config
     *
     * @param traceId transaction id from network request headers
     * @param context Context Object
     */
    public void setTraceIdKey(String traceId, Context context) {
        String traceIdKey = SharedPref.getConfigKeys(context, Config.KEYNAME_TRACE_ID, Config.KEYNAME_TRACE_ID_DEFAULT);
        if (!traceIdKey.equalsIgnoreCase(traceId)) {
            SharedPref.setConfigKeys(context, Config.KEYNAME_TRACE_ID, traceId);
        }
    }


    public static synchronized SecLibNG getInstance() {
        if (sSecLib == null) {
            sSecLib = new SecLibNG();
        }
        return sSecLib;
    }


    public boolean getSMAPIStatus(Context context) {

        if (context != null) {
            String currentSMAPIStatus = SharedPref.getConfigKeys(context, Config.KEYNAME_SMAPI_STATUS, "OFF");

            if (currentSMAPIStatus!=null && "ON".equalsIgnoreCase(currentSMAPIStatus))
                return true;
            else
                return false;
        }
        else
        {
            Logger.e(TAGSECLIB, "Cannot identify SMAPI status as context is null");
            return false;
        }
    }


    /**
     * Method to enable/disable SMAPI at runtime to be invoked by target application
     */
    public void turnSMAPIONOFF(Boolean enable, Context context) {

        String currentSMAPIStatus=SharedPref.getConfigKeys(context, Config.KEYNAME_SMAPI_STATUS, "OFF");

        if (!enable) {  //Disable SMAPI by checking the current status
            if("ON".equalsIgnoreCase(currentSMAPIStatus)) {
                Logger.i(TAGSECLIB, "Disabling SMAPI");

                SharedPref.setConfigKeys(context, Config.KEYNAME_SMAPI_STATUS, "OFF");
                mContext = null;
                turnOnAutoLifecycleEvents(null);

                if (db != null)
                    db.clearDb();
            }
            else
            {
                Logger.i(TAGSECLIB, "SMAPI is already OFF");
            }
        } else {
            if("OFF".equalsIgnoreCase(currentSMAPIStatus)) {
                Logger.i(TAGSECLIB, "Enabling SMAPI");

                SharedPref.setConfigKeys(context, Config.KEYNAME_SMAPI_STATUS, "ON");
                //Enable SMAPI data collection at runtime
                mContext = context.getApplicationContext();
                setContext(mContext);
            }
            else  {
                Logger.i(TAGSECLIB, "SMAPI is already ON");
            }
        }
    }


    /***
     * Set context for the SECLIB library.
     *
     * @param context Context object
     */
    public void setContext(Context context) {
        try {
            Logger.i(TAGSECLIB, "Setting the context ");
            if (context == null) {
                Logger.e(TAGSECLIB, "Context cannot be null");
            } else {

                String SMAPIStatus = SharedPref.getConfigKeys(context, Config.KEYNAME_SMAPI_STATUS, "ON");

                SharedPref.setConfigKeys(context, Config.KEYNAME_SEND_EVENT_SERVICE_RUNNING_STATUS, "finished");

                if (SMAPIStatus.equalsIgnoreCase("OFF")) {
                    mContext = null;
                    return;
                }
                else
                {
                    SharedPref.setConfigKeys(context, Config.KEYNAME_SMAPI_STATUS, "ON");
                }

                CrashHandler.init(context);

                int version = Build.VERSION.SDK_INT;
                if (version >= Config.MIN_SDK_LEVEL_FOR_SECLIB) {
                    Application application = (Application) context;
                    mContext = context.getApplicationContext();
                    if (Config.isOnline(context)) {
                        Intent intent = new Intent(context, EntryPointHandling.class).putExtra("action", Config.HOME_DOC);
                        EntryPointHandling.enqueueWork(context, intent);
                    } else {
                        SharedPref.setConfigKeys(mContext, Config.KEYNAME_HOME_DOC_RETRIEVED, Config.KEYNAME_HOME_DOC_RETRIEVED_NO);
                        Intent intent = new Intent(context, EntryPointHandling.class).putExtra("action", Config.DELETE_DATA);
                        EntryPointHandling.enqueueWork(context, intent);

                        Logger.i(TAGSECLIB, "Internet connection is unavailable.Setting flag for retrieval once user is online.");
                    }
                    initializeEncryption();
                    turnOnAutoLifecycleEvents(application);
                    createDb(application);
                } else {
                    Logger.i(TAGSECLIB, "Minimum API version should be " + Config.MIN_SDK_LEVEL_FOR_SECLIB + " Currently using " + version);
                }
                new NetworkCallRepo();
                new OkHttpCallRepo();
            }
        } catch (Exception e) {
            Logger.e(TAGSECLIB, "Exception while initializing SecLib library: " + e.getMessage(), e);
        }
    }

    /***
     * Get context
     *
     * @return Returns true context
     */
    public Context getAppContext() {
      return mContext;
    }

    /**
     * CreateDatabase to store Events
     *
     * @param context Context Object
     */
    private void createDb(Context context) {
        db = SqliteDb.getInstance(context);
    }

    /***
     * Create a custom event object with mandatory fields if element value are null or empty string it will replaced by NA
     *
     * @param eventElement     Event element
     * @param eventDescription Event description
     * @param pageName         PageName or Activity name
     * @param subPage          Sub page name if any
     * @return Event object with mandatory fields.
     */
    public static Event createCustomEvent(Context context, String eventElement, String eventDescription, String pageName, String subPage) {

        if (context == null) {
            Logger.e(TAGSECLIB, "Context value cannot be null");
            return null;
        }
        Event event = new Event(Event.EventType.UI_CUSTOM, context);
        event.addPayload(EventConstants.EVENT_DESCRIPTION, Config.nullOrEmptyValueCheckForCustomEvent(eventDescription));
        event.addPayload(EventConstants.EVENT_ELEMENT, Config.nullOrEmptyValueCheckForCustomEvent(eventElement));
        event.addPayload(EventConstants.X_VF_PAGE, Config.nullOrEmptyValueCheckForCustomEvent(pageName));
        event.addPayload(EventConstants.X_VF_SUB_PAGE, Config.nullOrEmptyValueCheckForCustomEvent(subPage));
        return event;
    }

    /**
     * logEvent Log the activity event that occurred.
     *
     * @param event type of event and its details
     */

    public void logEvent(Event event) {
        if (mContext == null || event == null) {
            return;
        }

        try {
            if (!Config.blackListStatus(event.getJSONObject(), Config.blackListString(mContext))) {
                ArrayList<Event> list = new ArrayList<>();
                if (!event.getJSONObject().has(EventConstants.EVENT_DESCRIPTION))
                    event.addPayload(EventConstants.EVENT_DESCRIPTION, Config.DEFAULT_NA);
                if (!event.getJSONObject().has(EventConstants.EVENT_ELEMENT))
                    event.addPayload(EventConstants.EVENT_ELEMENT, Config.DEFAULT_NA);
                if (!event.getJSONObject().has(EventConstants.X_VF_PAGE))
                    event.addPayload(EventConstants.X_VF_PAGE, Config.DEFAULT_NA);
                if (!event.getJSONObject().has(EventConstants.X_VF_SUB_PAGE))
                    event.addPayload(EventConstants.X_VF_SUB_PAGE, Config.DEFAULT_NA);

                event.addPayload(EventConstants.X_VF_TRACE_TID, Config.DEFAULT_NA);

                list.add(event);
                db.insertEvent(list, false);
            } else {
                Logger.i(TAGSECLIB, "Blacklisted Event : " + event.getJSONObject().toString());
            }
        } catch (JSONException n) {
            Logger.e(TAG_SECLIBNG, "Json Exception", n);
        } catch (NullPointerException n) {
            Logger.e(TAG_SECLIBNG, missingDb + n.getMessage(), n);
        }
    }

    /****
     * Log the event list
     *
     * @param event events in array list
     */
    public void logEvent(ArrayList<Event> event) {
        if (mContext == null || event == null) {
            return;
        }
        try {
            db.insertEvent(event, false);
        } catch (NullPointerException n) {
            Logger.e(TAG_SECLIBNG, missingDb + n.getMessage(), n);
        }
    }

    /**
     * Log events into sqlite
     *
     * @param eventElement     event element
     * @param eventDescription event description
     * @param subPage          Sub page name if any
     */
    public void logEventButton(String eventElement, String eventDescription, String subPage, Event.EventType eventType) {
        if (mContext == null)
            return;
        try {
            Event event1 = new Event(eventType, mContext);
            event1.addPayload(EventConstants.EVENT_DESCRIPTION, eventDescription.isEmpty() ? Config.DEFAULT_NA : eventDescription);
            event1.addPayload(EventConstants.EVENT_ELEMENT, eventElement.isEmpty() ? Config.DEFAULT_NA : eventElement);
            event1.addPayload(EventConstants.X_VF_PAGE, AutoLifecycleLogger.getCurrentActivity());
            String subPageName = AutoLifecycleLogger.getCurrentActivity().equalsIgnoreCase(subPage) ? "NA" : subPage;

            if (subPageName != null && subPageName.contains(Config.ERROR_TEXT))
                subPageName = "NA";

            event1.addPayload(EventConstants.X_VF_SUB_PAGE, subPageName);

            event1.addPayload(EventConstants.X_VF_TRACE_TID, Config.DEFAULT_NA);

            if (!Config.blackListStatus(event1.getJSONObject(), Config.blackListString(mContext))) {
                ArrayList<Event> list = new ArrayList<>();
                list.add(event1);
                db.insertEvent(list, false);
            } else {
                Logger.i(TAGSECLIB, "Blacklisted Event : " + event1.getJSONObject().toString());
            }
        } catch (JSONException je) {
            Logger.e(TAG_SECLIBNG, "Json Exception", je);
        } catch (NullPointerException n) {
            Logger.e(TAG_SECLIBNG, n.getMessage(), n);
        }
    }

    /**
     * Log events into sqlite for network requests
     *
     * @param eventElement     event element
     * @param eventDescription event description
     */
    public void logNetworkevent(String eventElement, String eventDescription, Event.EventType eventType, String traceId) {
        if (mContext == null)
            return;
        try {
            Event networkEvent = new Event(eventType, mContext);
            networkEvent.addPayload(EventConstants.EVENT_DESCRIPTION, eventDescription.isEmpty() ? Config.DEFAULT_NA : eventDescription);
            networkEvent.addPayload(EventConstants.EVENT_ELEMENT, eventElement.isEmpty() ? Config.DEFAULT_NA : eventElement);
            networkEvent.addPayload(EventConstants.X_VF_PAGE, AutoLifecycleLogger.getCurrentActivity());
            networkEvent.addPayload(EventConstants.X_VF_TRACE_TID, traceId);

            if (!Config.blackListStatus(networkEvent.getJSONObject(), Config.blackListString(mContext))) {
                ArrayList<Event> list = new ArrayList<>();
                list.add(networkEvent);
                db.insertEvent(list, false);
            } else {
                Logger.i(TAGSECLIB, "Blacklisted Event : " + networkEvent.getJSONObject().toString());
            }

        } catch (JSONException je) {
            Logger.e(TAG_SECLIBNG, "Json Exception", je);
        } catch (NullPointerException n) {
            Logger.e(TAG_SECLIBNG, n.getMessage(), n);
        }
    }


    /***
     * Log the event for exception and crash
     *
     * @param eventElement     event element
     * @param eventDescription event Description
     * @param subPage          Sub page name
     * @param stackTrace       Complete Stack trace for the exception
     */
    public void logEventException(String eventElement, String eventDescription, String subPage, String stackTrace, boolean isCrash) {

        if (mContext == null)
            return;
        try {

            Event event1 = new Event(isCrash ? Event.EventType.CRASH : Event.EventType.EXCEPTIONS, mContext);

            if (isCrash) {
                Logger.i(TAGSECLIB, "Crash encountered. Logging Exception as a Crash event. ");
                int deleteRowCount = db.deleteExceptionEvent();

                if (deleteRowCount != 1) {
                    Logger.i(TAGSECLIB, "Didn't find matching exception event. ");
                    return;
                }
            }

            if(!isCrash) {
                if (null != stackTrace && stackTrace.toLowerCase().contains(Config.SMAPI_PACKAGE_NAME)) {
                    eventDescription = eventDescription.isEmpty() ? "SMAPI Exception" : "SMAPI Exception:" + eventDescription;
                } else {
                    eventDescription = eventDescription.isEmpty() ? "Application Exception" : "Application Exception:" + eventDescription;
                }
            }

            event1.addPayload(EventConstants.EVENT_DESCRIPTION, eventDescription.isEmpty() ? Config.DEFAULT_NA : eventDescription);
            event1.addPayload(EventConstants.EVENT_ELEMENT, eventElement);
            event1.addPayload(EventConstants.X_VF_PAGE, AutoLifecycleLogger.getCurrentActivity());
            String subPageName = AutoLifecycleLogger.getCurrentActivity().equalsIgnoreCase(subPage) ? "NA" : subPage;

            if (subPageName != null && subPageName.contains(Config.ERROR_TEXT))
                subPageName = "NA";

            event1.addPayload(EventConstants.X_VF_SUB_PAGE, subPageName);
            event1.addPayload(EventConstants.X_VF_TRACE_STACK, stackTrace);

            event1.addPayload(EventConstants.X_VF_TRACE_TID, Config.DEFAULT_NA);

            /*Crash handling **/
            SharedPref.setConfigKeys(mContext, Config.KEYNAME_EXCEPTION_TRANSACTION_ID, event1.getTransactionId());

            if (!Config.blackListStatus(event1.getJSONObject(), Config.blackListString(mContext))) {
                ArrayList<Event> list = new ArrayList<>();
                list.add(event1);
                db.insertEventNonAsync(list, false);
                //Config.updateExceptionCount(mContext, false);
            } else {
                Logger.i(TAGSECLIB, "Blacklisted Event : " + event1.getJSONObject().toString());
            }
        } catch (JSONException je) {
            Logger.e(TAG_SECLIBNG, "Json Exception " + je.getMessage(), je);
        } catch (NullPointerException n) {
            Logger.e(TAG_SECLIBNG, n.getMessage(), n);
        } catch (Exception e) {
            Logger.e(TAG_SECLIBNG, e.getMessage(), e);
        }
    }

    /***
     * Set Environment for the Seclib
     *
     * @param environment Current Environment, It should be ENVIRONMENT_PRE or ENVIRONMENT_PRO
     * @param context     Context object
     */
    public void setEnvironment(String environment, Context context) {
        environmentString = environment;
        String currentEnvironment = SharedPref.getConfigKeys(context, Config.KEYNAME_ENVIRONMENT_VALUE_SAVED, ENVIRONMENT_PRE);
        if (!(currentEnvironment.equalsIgnoreCase(environmentString))) {
            //TODO Sets the environment value and updates the reset count
//            Config.updateResetCountReceived(context);
            SharedPref.setConfigKeys(context, Config.KEYNAME_ENVIRONMENT_VALUE_SAVED, environment);

        }
    }

    public String getEnvironment() {
        return environmentString;
    }

    /**
     * Log the Activity Events from activities.
     *
     * @param app App
     */
    private void turnOnAutoLifecycleEvents(final Application app) {
        if (mContext == null) {
            return;
        }
        if (app == null) {
            Logger.w("SecLibNG.turn automatic", "Application object is null");
            return;
        }
        if (mAutoLogManager != null) {
            turnOffAutoLifecycleEvents();
            mAutoLogManager = null;
        }
        mAutoLogManager = new AutoLogManager(app);
        mAutoLogManager.registerActivityLifecycleCallbacks();
    }


    /***
     * Turn off auto life cycle events for activity
     */
    private void turnOffAutoLifecycleEvents() {
        if (mContext == null) {
            return;
        }
        mAutoLogManager.unregisterActivityLifecycleCallbacks();
    }

    /***
     * Initialize Encryption details
     */
    private void initializeEncryption() {
        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {

                if (KeytoolHelper.getPublicKey(mContext).equalsIgnoreCase(KeytoolHelper.EMPTY_STRING)) {
                    uUid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                    SharedPref.setEncryptionKeyStatus(mContext, true);
                    CreateKeyStoreAsync createKeyStoreAsync = new CreateKeyStoreAsync(0, mContext);
                    createKeyStoreAsync.execute(uUid);
                } else {
                    if (KeytoolHelper.getDecKey() == null) {
                        SharedPref.setEncryptionKeyStatus(mContext, true);

                        DecryptionAsync decryptionAsync = new DecryptionAsync(mContext);
                        decryptionAsync.execute();
                    }
                }
            }
        });
        thr.start();
    }

    private  Intent intent;

    /***
     * Method to implement flush of events
     */
    public void flush() {

        if (mContext != null && Config.isOnline(mContext)) {
            String flushCurrentStatus = SharedPref.getConfigKeys(mContext, Config.KEYNAME_SEND_EVENT_SERVICE_RUNNING_STATUS, "finished");

            if(!"running".equalsIgnoreCase(flushCurrentStatus))
            {/* Start the service now. **/
                Logger.i(TAGSECLIB, "SMAPI events are flushed explicitly through target application");

                intent = new Intent(mContext, EventsIntentService.class).putExtra("action", Config.NORMAL_EVENTS);

                EventsIntentService.enqueueWork(mContext, intent);
            }
            else
            {
                Logger.i(TAGSECLIB, "SMAPI events CANNOT be flushed currently as it is already in progress.");
            }
        } else if (mContext != null) {
            Logger.e(TAGSECLIB, "Device is offline. Events cannot be flushed.");
        }
    }

    /***
     * Print Logs in console or not
     *
     * @param status true if all the logs has to print
     */
    public void setVerbose(boolean status) {
        verboseStatus = status;
    }

    /***
     * Get log status to print or not.
     *
     * @return Returns true if log has to print,Else false.
     */
    public boolean getVerboseStatus() {
        return verboseStatus;
    }


    /***
     * Set package name  for the Seclib
     *
     * @param traceSourceName application package name
     * @param context     Context object
     */

    public void setTraceSource(Context context , String traceSourceName){
        if(context !=null &&TextUtils.isEmpty(getTraceSource(context))){
            SharedPreferences.Editor ed = getSharedPreferences(context).edit();
            ed.putString(EventConstants.X_VF_TRACE_SOURCE, traceSourceName);
            ed.apply();
        }
    }


    /***
     * Returns the trace source from the SharedPreferences
     *
     * @return trace source String
     */
    public String getTraceSource(Context context){
       if(context!=null){
           return getSharedPreferences(context).getString(EventConstants.X_VF_TRACE_SOURCE, "");
       }
       return Config.DEFAULT_NA;
    }

    /***
     * Returns the SharedPreferences
     *
     * @return SharedPreferences
     */

    private SharedPreferences getSharedPreferences(Context context){
        if(sharedPreferences == null){
            sharedPreferences = context.getSharedPreferences(SharedPref.SHARED_PREFERENCE_NAME, 0);
        }
        return sharedPreferences;
    }

}
