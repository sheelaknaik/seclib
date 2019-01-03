package com.vodafone.lib.seclibng;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.vodafone.lib.seclibng.auto.AutoLifecycleLogger;
import com.vodafone.lib.seclibng.comms.Config;
import com.vodafone.lib.seclibng.comms.EventConstants;
import com.vodafone.lib.seclibng.comms.EventHeaders;
import com.vodafone.lib.seclibng.comms.Logger;
import com.vodafone.lib.seclibng.comms.SharedPref;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created and initialize events
 */

public class Event {
    private final String TAG_EVENT = "Event.addPayload";
    private static ArrayList<Event> pendingEvents = null;
    private JSONObject mRootObject;
    private EventType mEventType;
    private String id = "";

    /****
     * Event type that can be used for event event initialization
     */
    public enum EventType {
        PAGE("Page"),
        APPLICATION("Application"),
        UI_CLICKS("UIControl"),
        UI_SWIPE("UISwipe"),
        UI_CUSTOM("UICustom"),
        NOTIFICATION("Notification"),
        UI_CHANGE("UIChange"),
        EXCEPTIONS("Exception"),
        NETWORK("Network"),
        CRASH("Crash");

        private String value;

        EventType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /***
     * Event Constructor use to initialize the generic metrics.
     *
     * @param eventType Event type
     * @param context   Context Object
     */
    @SuppressLint("MissingPermission")
    public Event(EventType eventType, Context context) {
        NetworkInfo networkInfo = null;
        mEventType = eventType;
        mRootObject = new JSONObject();
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            networkInfo = connMgr.getActiveNetworkInfo();
        } catch (SecurityException e) {
            Logger.d("Event.java", "Missing permission,android.permission.ACCESS_NETWORK_STATE", e);
        }
        try {
            mRootObject.put(EventConstants.X_VF_EVENT_TYPE, eventType.getValue());
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            mRootObject.put(EventConstants.DEVICE_ORIENTATION, (display.getRotation() == Surface.ROTATION_0 || display.getRotation() == Surface.ROTATION_180) ? "Portrait" : "Landscape");
            mRootObject.put(EventConstants.X_VF_TRACE_TRANSACTION_ID, UUID.randomUUID().toString());
            mRootObject.put(EventConstants.X_VF_TRACE_TIMESTAMP, Long.toString(System.currentTimeMillis()));
            mRootObject.put(EventConstants.X_VF_TRACE_SESSION_ID, (AutoLifecycleLogger.getsSessionId() != null) ? AutoLifecycleLogger.getsSessionId() : SharedPref.getConfigKeys(context, Config.KEYNAME_LAST_SESSION, ""));
            mRootObject.put(EventConstants.X_VF_NET_TYPE, EventHeaders.getNetworkTypeName(context));
            if (networkInfo != null) {
                mRootObject.put(EventConstants.X_VF_NET_BAND, networkInfo.getSubtypeName().isEmpty() ? Config.DEFAULT_NA : networkInfo.getSubtypeName());
            } else {
                mRootObject.put(EventConstants.X_VF_NET_BAND, Config.DEFAULT_NA);
            }
        } catch (JSONException je) {
            Logger.e("Event()", "Could not put event type!", je);
        }
    }

    /**
     * Event Constructor used for initialize events from Sqlite
     *
     * @param id         Row id for the event after inserting data to sqlite
     * @param jsonObject Json Object to initialize
     */
    public Event(String jsonObject, String id) {
        this.id = id;
        try {
            mRootObject = new JSONObject(jsonObject);
        } catch (JSONException e) {
            Logger.e(TAG_EVENT, "Exception while reinitializing events from sqlite data", e);
        }

    }


    /***
     * Pending events that missed to be added in database
     *
     * @return list of event
     */
    public static List<Event> getPendingEvents() {
        return pendingEvents;
    }

    /***
     * Add item to pending events
     *
     * @param pendingEvent Events that missed to add in database
     */
    public static void setPendingEvents(List<Event> pendingEvent) {
        if (pendingEvents == null) {
            pendingEvents = new ArrayList<>();
        }
        Event.pendingEvents.addAll(pendingEvent);
    }

    /***
     * Removing an event from the list if it is added to sqlite
     */
    public static void clearEventArray() {
        if (pendingEvents != null) {
            pendingEvents.clear();
            pendingEvents = null;
        }
    }

    /***
     * Add payload to event
     *
     * @param key   event key name
     * @param value event value-String
     */
    public void addPayload(String key, String value) {
        try {
            mRootObject.put(key, value);
        } catch (JSONException je) {
            Logger.e(TAG_EVENT, "Could not put String as payload!", je);
        }
    }

    /***
     * Add payload to event
     *
     * @param key   event key name
     * @param value event value-String
     */
    public void addCustomPayload(String key, String value) {
        try {
            if (key == null) {
                return;
            }
            String CUSTOM_EVENT_PREFIX = "x-vf-custom-";
            mRootObject.put(CUSTOM_EVENT_PREFIX + Config.nullOrEmptyValueCheckForCustomEvent(key), Config.nullOrEmptyValueCheckForCustomEvent(value));
        } catch (JSONException je) {
            Logger.e(TAG_EVENT, "Could not put String as payload!", je);
        }
    }

    /***
     * Returns the Event in JSONObject format
     *
     * @return JSON object
     */
    public JSONObject getJSONObject() {
        return mRootObject;
    }

    /***
     * Returns the Event type
     *
     * @return Returns the EventType
     */
    public EventType getEventType() {
        return mEventType;
    }

    /***
     * Returns the Event id which is associated with each event which is stored in db
     *
     * @return Event Id
     */
    public String getEventId() {
        return id;
    }


    /***
     * Returns the time stamp from the event which is created
     *
     * @return returns timestamp
     * @throws Exception throws exception
     */
    public String getTimeStamp() throws Exception {
        return mRootObject.getString(EventConstants.X_VF_TRACE_TIMESTAMP);
    }

    /***
     * Returns the event-element from the event which is created
     *
     * @return event element
     * @throws Exception throws exception
     */
    public String getEventElement() throws Exception {
        return mRootObject.getString(EventConstants.EVENT_ELEMENT);
    }

    /***
     * Returns the page name from the event which is created
     *
     * @return page name
     * @throws Exception exception
     */
    public String getPageName() throws Exception {
        return mRootObject.getString(EventConstants.X_VF_PAGE);
    }


    /***
     * Returns the page name from the event which is created
     *
     * @return sub page name
     * @throws Exception exception
     */
    public String getSubPageName() throws Exception {
        return mRootObject.getString(EventConstants.X_VF_SUB_PAGE);
    }

    /***
     * Returns the Event Description from the event which is created
     *
     * @return event description
     * @throws Exception exception
     */
    public String getEventDescription() throws Exception {
        return mRootObject.getString(EventConstants.EVENT_DESCRIPTION);
    }
    //, "device-orientation", "x-vf-trace-transaction-id", "x-vf-trace-session-id", "x-vf-net-type", "x-vf-net-band"]

    /***
     * Returns the Device orientation from the event which is created
     *
     * @return device orientation
     * @throws Exception exception
     */
    public String getDeviceOrientation() throws Exception {
        return mRootObject.getString(EventConstants.DEVICE_ORIENTATION);
    }

    /***
     * Returns the transaction ID from the event which is created
     *
     * @return transaction id
     * @throws Exception exception
     */
    public String getTransactionId() throws Exception {
        return mRootObject.getString(EventConstants.X_VF_TRACE_TRANSACTION_ID);
    }

    public void setEventType(String eventType) throws Exception {
        //return mRootObject.setString(EventConstants.X_VF_TRACE_TRANSACTION_ID);
        mRootObject.remove(EventConstants.X_VF_EVENT_TYPE);
        mRootObject.put(EventConstants.X_VF_EVENT_TYPE,eventType);
    }


    /***
     * Returns the Session ID from the event which is created
     *
     * @return session id
     * @throws Exception exception
     */
    public String getSessionId() throws Exception {
        return mRootObject.getString(EventConstants.X_VF_TRACE_SESSION_ID);
    }

    /***
     * Returns the Net Type from the event which is created
     *
     * @return net type
     * @throws Exception exception
     */
    public String getNetType() throws Exception {
        return mRootObject.getString(EventConstants.X_VF_NET_TYPE);
    }

    /***
     * Returns the Net Band from the event which is created
     *
     * @return net band
     * @throws Exception exception
     */
    public String getNetBand() throws Exception {
        return mRootObject.getString(EventConstants.X_VF_NET_BAND);
    }

}
