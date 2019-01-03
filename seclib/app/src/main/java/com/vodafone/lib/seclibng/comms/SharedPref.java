package com.vodafone.lib.seclibng.comms;


import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.Set;
import java.util.UUID;

public class SharedPref {
    public static final String SHARED_PREFERENCE_NAME = "config";
    private static final String SHA_KEYS = "shakeys";
    private static final String KEYNAME_CURRENT_EVENT_COUNT_IN_DB = "currentEventCountinDb";
    private static final String KEYNAME_INSTALL_ID = "InstallId";
    private static final String KEYNAME_MAX_NO_OF_EVENTS_SENT = "max_number_of_events_sent";
    private static final String KEYNAME_MAX_NO_OF_EVENTS_SENT_START_DATE = "max_number_of_events_sent_start_date";
    private static final String KEYNAME_EXCEPTION_COUNT = "exception_count";
    public static final String KEYNAME_MAX_NO_OF_EXCEPTIONS = "max_number_of_exceptions";
    private static final String KEYNAME_MAX_NO_OF_RESETS_RECEIVED = "max_number_of_resets_received";
    private static final String KEYNAME_MAX_NO_OF_RESETS = "max_number_of_resets";
    private static final String DEFAULT_MAX_NO_OF_RESET = "10";
    private static final String KEYNAME_ENCRYPTION_KEY_STATUS = "encryption_key_status";
    private static final String ENCRYPTION_KEY_YET_TO_CREATE = "encryption_key_yet_to_create";
    private static final String ENCRYPTION_KEYGENERATION_FAILED = "encryption_keygeneration_failed";


    /***
     * Initialize key for encryption
     *
     * @param key     decrypted key
     * @param context Context object
     */
    public static void setKeys(Set<String> key, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_NAME, 0);
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putStringSet(SHA_KEYS, key);
        ed.apply();
    }


    /***
     * ADD VALUES TO SharedPreference for configuration change
     *
     * @param key     Key name has to save into the Shared preference
     * @param value   The value has to save into the shared preference for each key
     * @param context Context object
     */
    public static void setConfigKeys(Context context, String key, String value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_NAME, 0);
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putString(key, value);

        ed.apply();
    }

    /***
     * Get key for encryption
     *
     * @param context Context object
     * @return String returns the SHA keys
     */
    public static Set getKeys(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_NAME, 0);
        return sharedPreferences.getStringSet(SHA_KEYS, null);
    }

    /***
     * Get values shared in the SharedPreference
     *
     * @param context     Context Object
     * @param key         Key name for the shared Preference for the value
     * @param returnValue Default return value for the shared preference key
     * @return The value for the particular key from shared preference
     */


    public static String getConfigKeys(Context context, String key, String returnValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_NAME, 0);
        return sharedPreferences.getString(key, returnValue);
    }


    /***
     * Get Current Event count from database which is stored in shared Preference.
     *
     * @param context Context object
     * @return Returns the count
     */
    public static long getEventCountInDatabase(Context context) {
        SharedPreferences sf = context.getSharedPreferences(SHARED_PREFERENCE_NAME, 0);
        return sf.getLong(KEYNAME_CURRENT_EVENT_COUNT_IN_DB, 0);
    }

    /***
     * Set the database event count in shared preference by incrementing 1
     *
     * @param context Context object
     * @param count   count to increase or the available count in table.
     */
    public static void setEventCountInDatabase(Context context, long count, boolean remove) {
        SharedPreferences sf = context.getSharedPreferences(SHARED_PREFERENCE_NAME, 0);
        SharedPreferences.Editor editor = sf.edit();
        long currentCount = (remove) ? sf.getLong(KEYNAME_CURRENT_EVENT_COUNT_IN_DB, 0) - count : sf.getLong(KEYNAME_CURRENT_EVENT_COUNT_IN_DB, 0) + count;
        editor.putLong(KEYNAME_CURRENT_EVENT_COUNT_IN_DB, currentCount);
        editor.apply();
    }

    /***
     * Get the install id for the Event Header,Install id is generated for the first time if it is not yet available.
     *
     * @param context Context Object
     * @return The install-id which is either generated or already saved.
     */
    public static String getInstallId(Context context) {
        String installId = "";
        SharedPreferences sf = context.getSharedPreferences(SHARED_PREFERENCE_NAME, 0);
        installId = sf.getString(KEYNAME_INSTALL_ID, "nil");
        if ("nil".equalsIgnoreCase(installId)) {
            SharedPreferences.Editor editor = sf.edit();
            installId = UUID.randomUUID().toString();
            editor.putString(KEYNAME_INSTALL_ID, UUID.randomUUID().toString());
            editor.apply();
        }
        return installId;
    }


    /***
     * Updates the number of events sent in a reset period.
     *
     * @param count   Number of event sent
     * @param context Context value
     */
    public static void updateCurrentEventSentCount(int count, Context context, boolean reset) {
        SharedPreferences sf = context.getSharedPreferences(SHARED_PREFERENCE_NAME, 0);
        int currentCount = sf.getInt(KEYNAME_MAX_NO_OF_EVENTS_SENT, 0);
        SharedPreferences.Editor ed = sf.edit();
        if (reset) {
            Calendar calendar = Calendar.getInstance();
            ed.putString(KEYNAME_MAX_NO_OF_EVENTS_SENT_START_DATE, Long.toString(calendar.getTimeInMillis()));
            ed.putInt(KEYNAME_MAX_NO_OF_EVENTS_SENT, 0);
            ed.apply();
        } else {
            currentCount = currentCount + count;
            ed.putInt(KEYNAME_MAX_NO_OF_EVENTS_SENT, currentCount);
            ed.apply();
        }
    }

    /***
     * Number of events sent in a reset period.
     *
     * @param context Context value
     * @return The number of events sent
     */
    public static int getCurrentEventsSentCount(Context context) {
        SharedPreferences sf = context.getSharedPreferences(SHARED_PREFERENCE_NAME, 0);
        return sf.getInt(KEYNAME_MAX_NO_OF_EVENTS_SENT, 0);
    }

    /***
     * Update the exception count in shared preference
     *
     * @param context Context object
     * @param reset   reset the count to zero or increment the count by one,true to reset the exception count to zero
     */
    public static void updateExceptionCount(Context context, @SuppressWarnings("SameParameterValue") boolean reset) {
        SharedPreferences sf = context.getSharedPreferences(SHARED_PREFERENCE_NAME, 0);
        int currentCount = sf.getInt(KEYNAME_EXCEPTION_COUNT, 0);
        SharedPreferences.Editor ed = sf.edit();
        if (reset) {
            ed.putInt(KEYNAME_EXCEPTION_COUNT, 0);
        } else {
            currentCount++;
            ed.putInt(KEYNAME_EXCEPTION_COUNT, currentCount);
        }
        ed.apply();
    }

    /***
     * Number of events sent in a reset period.
     *
     * @param context Context value
     * @return The number of events sent
     */
    public static boolean isReachedMaximumNoOfException(Context context) {
        SharedPreferences sf = context.getSharedPreferences(SHARED_PREFERENCE_NAME, 0);
        int maxExceptionCount = Integer.parseInt(sf.getString(KEYNAME_MAX_NO_OF_EXCEPTIONS, "10"));
        int exceptionCountReceived = sf.getInt(KEYNAME_EXCEPTION_COUNT, 0);
        return exceptionCountReceived > maxExceptionCount;
    }

    /***
     * Updates the value for max-number_of_reset
     *
     * @param context Context value
     */
    public static void updateResetCountReceived(Context context) {
        SharedPreferences sf = context.getSharedPreferences(SHARED_PREFERENCE_NAME, 0);
        int currentCount = sf.getInt(KEYNAME_MAX_NO_OF_RESETS_RECEIVED, 0);
        SharedPreferences.Editor editor = sf.edit();
        currentCount++;
        editor.putInt(KEYNAME_MAX_NO_OF_RESETS_RECEIVED, currentCount);
        editor.apply();
    }

    /***
     * Resets the value for max-number_of_reset
     *
     * @param context Context value
     */
    public static void updateResetCountToZero(Context context) {
        SharedPreferences sf = context.getSharedPreferences(SHARED_PREFERENCE_NAME, 0);
        SharedPreferences.Editor editor = sf.edit();
        editor.putInt(KEYNAME_MAX_NO_OF_RESETS_RECEIVED, 0);
        editor.apply();
    }

    /***
     * Checks whether the reset count exceeded the max_event_reset.Returns true or false
     *
     * @param context Context value
     * @return boolean, count has been exceeded or not
     */
    public static boolean getResetCountStatusBlockEvent(Context context) {
        SharedPreferences sf = context.getSharedPreferences(SHARED_PREFERENCE_NAME, 0);
        int currentCount = sf.getInt(KEYNAME_MAX_NO_OF_RESETS_RECEIVED, 0);
        boolean countStatus = (currentCount > Integer.parseInt(sf.getString(KEYNAME_MAX_NO_OF_RESETS, DEFAULT_MAX_NO_OF_RESET)));
        //TODO Reset count has been disabled for this version of smapi.If you want to enable comment out the below line.
        //return countStatus;
        return false;
    }

    /***
     * Sets the Keystore creation status.
     * @param context Application context
     * @param isAvailable true,if trying to  create keystore or already created ,else fail
     */
    public static void setEncryptionKeyStatus(Context context, boolean isAvailable) {
        SharedPref.setConfigKeys(context, KEYNAME_ENCRYPTION_KEY_STATUS, isAvailable ? ENCRYPTION_KEY_YET_TO_CREATE : ENCRYPTION_KEYGENERATION_FAILED);
    }

    /***
     * Retrieves the Keystore creation status.If it failed to create keystore, returns false else true.
     * @param context Application context
     * @return Boolean
     */
    public static boolean getEncryptionKeyStatus(Context context) {
        //noinspection RedundantConditionalExpression
        return (ENCRYPTION_KEYGENERATION_FAILED.equalsIgnoreCase(SharedPref.getConfigKeys(context, KEYNAME_ENCRYPTION_KEY_STATUS, ENCRYPTION_KEY_YET_TO_CREATE))) ? false : true;
    }


}
