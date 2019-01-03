package com.vodafone.lib.seclibng.comms;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.vodafone.lib.seclibng.internal.AlarmReceiver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

/**
 * Config class contains Constants and Globally used static methods.
 */

public class Config {

    public static final String HEADDER_STRING = "Public-Key-Pins";
    public static final String HOME_DOC = "homedoc";
    public static final String SETTINGS = "settings";
    public static final String DELETE_DATA = "deletedata";
    public static final String NO_USER_ID_DATA = "no_user_id_data";
    public static final String NORMAL_EVENTS = "normal_events";
    public static final String PAGE_LOAD_TIME = "Load Time";
    public static final String DEFAULT_MAX_NO_OF_EVENTS = "3000";
    public static final String DEFAULT_RESULT_SUCCESS = "success";
    public static final String DEFAULT_RESULT_FAIL = "fail";
    private static final String DEFAULT_START_SERVICE_DELAY = "1";
    public static final String DEFAULT_MAX_NO_EVENTS_IN_DB = "1000";
    public static final String DEFAULT_ID_EVENT_AGE = "10";
    public static final String DEFAULT_MAX_NO_USER_ID_EVENT_AGE = "5";
    public static final String KEY_PUBLIC_KEY_ENCRYPTED = "config";
    public static final String KEYTOOL_NAME = "seclib";
    static final String KEY_DEFAULT = "nil";
    public static final String KEYNAME_MAX_NO_OF_EVENTS = "max_number_of_events";
    public static final String KEYNAME_LAST_SESSION = "last_session_id";
    static final String KEYNAME_START_DATE_FOR_SETTINGS_7_DAYS = "START_DATE_FOR_SETTINGS";
    public static final String KEYNAME_MAX_NO_OF_EXCEPTIONS = "max_number_of_exceptions";
    private static final String KEYNAME_SERVICE_START_DELAY = "service_start_delay";
    public static final String KEYNAME_MAX_EVENTS_IN_DATABASE = "max_events_in_database";
    public static final String KEYNAME_MAX_EVENT_AGE = "max_event_age";
    public static final String KEYNAME_MAX_NO_USER_ID_EVENT_AGE = "no_user_id_event_age";
    private static final String KEYNAME_BLACKLIST = "blacklist";
    public static final String KEYNAME_USER_ID = "userid";
    public static final String KEYNAME_USER_ID_UNKNOWN = "Unknown";
    public static final String KEYNAME_DB_LAST_CHECKED_DATE = "last_checked_date_for_db_deletion";
    public static final String KEYNAME_SUBMIT_EVENTS = "http://a42.vodafone.com/rels/sec/submit-events";
    public static final String KEYNAME_SETTINGS_EVENT = "http://a42.vodafone.com/rels/sec/settings";
    public static final String KEYNAME_ENVIRONMENT_VALUE_SAVED = "environment_value_saved";
    public static final String KEYNAME_HOME_DOC_RETRIEVED = "homedoc_retrieved";
    public static final String KEYNAME_HOME_DOC_RETRIEVED_NO = "homedoc_retrieved_no";
    public static final String KEYNAME_HOME_DOC_RETRIEVED_YES = "homedoc_retrieved_yes";
    public static final String KEYNAME_ALARM_FLAG = "alarmflag";
    private static final String KEYNAME_ALARM_FLAG_SET = "alarm_flag_set";
    public static final String KEYNAME_ALARM_FLAG_RESET = "alarm_flag_reset";
    public static final String KEYNAME_SETTINGS_FLAG = "settingsflag";
    public static final String KEYNAME_SETTINGS_SET = "settings_flag_set";
    static final String KEYNAME_SETTINGS_RESET = "settings_flag_reset";
    static final String KEYNAME_HOME_DOC_RUNNING = "homedoc_call";
    static final String KEYNAME_HOME_DOC_RUNNING_YES = "homedoc_call_ongoing";
    static final String KEYNAME_HOME_DOC_RUNNING_NO = "homedoc_call_stopped";
    public static final String KEYNAME_SEND_EVENT_SERVICE_RUNNING_STATUS = "send_event_service_running_status";
    public static final String KEYNAME_ENTRY_POINT_SERVICE_RUNNING_STATUS = "entry_point_service_running_status";
    public static final String KEYNAME_SEND_EVENT_SERVICE_RESULT_STATUS = "send_event_service_result_status";
    public static final String DEFAULT_NA = "NA";
    static boolean KEYNAME_RESEND_EVENTS = false;
    public static final int MIN_SDK_LEVEL_FOR_SECLIB = 19;
    private static final String TAG_CONFIG = "Config";
    public static final int MAX_KEYGEN_RETRY_COUNT = 3;
    public static final String KEYNAME_SMAPI_STATUS = "KEYNAME_SMAPI_STATUS";

    public static final String KEYNAME_TRACE_ID_DEFAULT = "vf-trace-transaction-id";
    public static final String KEYNAME_TRACE_ID = "trace-id";

    public static final String chuck_body_unexpected_eof = "\n\n--- Unexpected end of content ---";
    public static final String chuck_body_content_truncated = "\n\n--- Content truncated ---";

    public static final String ERROR_TEXT = "Exception";

    public static final String APP_FOREGROUND = "Application enters foreground";
    public static final String APP_BACKGROUND = "Application enters background";

    public static final String KEYNAME_EXCEPTION_TRANSACTION_ID = "ExceptionTransactionId";

    public static final String QUERY_DELETE_EXCEPTION = "id = (SELECT id FROM SecLibEvents WHERE event_type LIKE \"EXCEPTIONS\" ORDER BY event_creation_time DESC LIMIT 1);";

    public static final String SMAPI_PACKAGE_NAME = "com.vodafone.lib.seclibng";

    public static final String COMPOUND_BUTTON_STATE_ON = "ON";
    public static final String COMPOUND_BUTTON_STATE_OFF = "OFF";

    public static final String DEVICE_LABEL = ";Device:";
    public static final String MANUFACTURER_LABEL = ";Manufacturer:";

    public static final String KEYNAME_WIFI = "WiFi";
    public static final String KEYNAME_Mobile = "Mobile";
    public static final String KEYNAME_TWOG = "2G";
    public static final String KEYNAME_THREEG = "3G";
    public static final String KEYNAME_FOURG = "4G";

    public static final String KEYNAME_ANDROID = "android:";


    /*Testing */
    public static final String KEYNAME_HOMEDOC_RUNNING = "homedoc_call";
    public static final String KEYNAME_HOMEDOC_RUNNING_YES = "homedoc_call_ongoing";
    public static final String KEYNAME_HOMEDOC_RUNNING_NO = "homedoc_call_stopped";
    public static final String KEYNAME_SUBMIT_EVENTS_ANONYMOUSLY = "http://a42.vodafone.com/rels/sec/submit-events-anonymously";
    public static final String HOMEDOC = "homedoc";
    public static final String KEYNAM_ERROR_REPEAT_PERIOD = "error_repeat_period";

    /***
     * Default private constructor
     */
    Config() {
    }


    /***
     * Returns the black status pf each event
     *
     * @param jobEvent    Event to be added
     * @param jConditions Blacklist conditions
     * @return Returns whether the event is blacklisted or not in boolean.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean blackListStatus(JSONObject jobEvent, JSONArray jConditions) throws JSONException {

        boolean isEventBlacklisted = false;
        for (int i = 0; i < jConditions.length(); i++) {
            JSONObject jConditionSingle = jConditions.getJSONObject(i);

            for (Iterator<String> jsonKeys = jConditionSingle.keys(); jsonKeys.hasNext(); ) {
                String key = jsonKeys.next();
                isEventBlacklisted = matching(jobEvent, key, jConditionSingle.getString(key));
                if (!isEventBlacklisted) {
                    break;
                }
            }
            if (isEventBlacklisted) {
                Logger.i(TAG_CONFIG, "Blacklisted Condition : " + jConditionSingle.toString());
                break;
            }
        }
        return isEventBlacklisted;
    }

    /***
     * Checking with black listed Json array settings
     *
     * @param job   JSon object from the event
     * @param key   Settings key
     * @param value Settings Value
     * @return True if matched
     * @throws JSONException throws exception
     */
    private static boolean matching(JSONObject job, String key, String value) throws JSONException {
        boolean matching;
        if (job.has(key)) {
            matching = (job.getString(key).equalsIgnoreCase(value));
            if (matching) {

                return true;
            }
            matching = checkWildCard(value, job.getString(key));
            if (matching) {
                return true;
            }
        }
        return false;
    }

    /***
     * Wild card checking with blacklist items
     *
     * @param key   Key to check
     * @param value value to compare
     * @return if matches return true
     */
    private static boolean checkWildCard(String key, String value) {
        boolean addtodb = false;

        String regex = key.replace("*", "[\\d\\s\\w-]*");
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(value);
        addtodb = m.find();
        if (addtodb) {
            addtodb = (value.length() == m.group().length());
        }

        return addtodb;
    }



    /***
     * Get the Black list contents in JSon array format from the shared preference which retrieved from Settings API
     *
     * @param context Context Value
     * @return Returns Blacklist settings in json array.
     */
    public static JSONArray blackListString(Context context) {
        JSONArray jarArray = new JSONArray();
        try {
            String st = SharedPref.getConfigKeys(context, KEYNAME_BLACKLIST, "[]");
            return new JSONArray(st);
        } catch (Exception e) {
            Logger.e(TAG_CONFIG, "Json Exception " + e.getMessage(), e);
        }
        return jarArray;
    }

    /**
     * Schedule the alarm/Job scheduler for sending the event
     */
    public static void setAlarm(Context context) {
        final int RQS_1 = 1;

        if (!SharedPref.getResetCountStatusBlockEvent(context)) {
            if (SharedPref.getConfigKeys(context, Config.KEYNAME_ALARM_FLAG, KEYNAME_ALARM_FLAG_RESET).equals(KEYNAME_ALARM_FLAG_RESET)) {
                int currentApiVersion = Build.VERSION.SDK_INT;

                if (currentApiVersion >= 21) {
                    jobScheduler(context);
                } else {
                    AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                    Intent intent = new Intent(context, AlarmReceiver.class);
                    intent.putExtra("action", NORMAL_EVENTS);

                    PendingIntent alarmIntent = PendingIntent.getBroadcast(context, RQS_1, intent, PendingIntent.FLAG_ONE_SHOT);

                    String alarmInterval = SharedPref.getConfigKeys(context, Config.KEYNAME_SERVICE_START_DELAY, DEFAULT_START_SERVICE_DELAY);

                    final long serviceStartDelay = Long.parseLong(alarmInterval) * 60000;

                    if (alarmMgr != null) {
                        if (currentApiVersion < Build.VERSION_CODES.KITKAT) {
                            alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + serviceStartDelay, alarmIntent);
                        } else {
                            if (currentApiVersion < Build.VERSION_CODES.M) {
                                alarmMgr.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + serviceStartDelay, alarmIntent);
                            } else {
                                alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + serviceStartDelay, alarmIntent);
                            }
                        }
                        Logger.i(TAG_CONFIG, "An alarm will be fired after " + alarmInterval + " Min");
                        SharedPref.setConfigKeys(context, KEYNAME_ALARM_FLAG, KEYNAME_ALARM_FLAG_SET);
                    } else {
                        Logger.e(TAG_CONFIG, "Alarm manager is null");

                    }
                }
            }
        }
    }

    /***
     * Checks whether the device is connected with internet or not.
     *
     * @param context Context object
     * @return If connected returns true,else False.
     */
    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean value = false;
        try {
            @SuppressLint("MissingPermission") NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                value = true;
            }
        } catch (SecurityException e) {
            Logger.e(TAG_CONFIG, "Missing permission: android.permission.ACCESS_NETWORK_STATE", e);
        }
        return value;

    }

    /***
     * Checks response code from the server and returns the result
     *
     * @param responseCode Response code from the server
     * @param connection   HttpUrl Connection
     * @return Returns the result
     * @throws ProtocolException throws exception
     */
    public static String checkCode(int responseCode, HttpsURLConnection connection) throws ProtocolException {
        BufferedReader bufferedReader = null;
        try (InputStream inputStream = connection.getInputStream()) {
            if (responseCode >= 200 && responseCode < 400) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                StringBuilder result = new StringBuilder();
                while ((line = bufferedReader.readLine()) != null)
                    result.append(line);
                inputStream.close();

                return result.toString();
            } else {
                try {
                    if (connection != null && connection.getResponseMessage() != null) {
                        String errorStream = "";
                        InputStream stream = null;
                        stream = connection.getErrorStream();

                        if (stream != null) {
                            Scanner scanner = new Scanner(stream);
                            scanner.useDelimiter("\\Z");
                            errorStream = scanner.next();
                        }
                        throw new ProtocolException("Error " + responseCode + "  " + connection.getResponseMessage() + " " + errorStream);
                    } else
                        throw new ProtocolException("Error " + responseCode);
                } catch (IOException e) {
                    Logger.e(TAG_CONFIG, "IO Exception " + e.getMessage(), e);
                }
            }
        } catch (IOException pe) {
            throw new ProtocolException("Error " + pe.getMessage());
        } finally {
            if (bufferedReader != null)
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    Logger.e(TAG_CONFIG, "IO Exception " + e.getMessage(), e);
                }
            if (connection != null)
                connection.disconnect();
        }
        return null;
    }

    /***
     * Job scheduler for triggering the service (Only for devices with API version >21)
     *
     * @param context Context value
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void jobScheduler(Context context) {
        try {

            String SMAPIStatus = SharedPref.getConfigKeys(context, Config.KEYNAME_SMAPI_STATUS, "ON");
            String flushCurrentStatus = SharedPref.getConfigKeys(context, Config.KEYNAME_SEND_EVENT_SERVICE_RUNNING_STATUS, "finished");

            if (SMAPIStatus.equalsIgnoreCase("ON") && !"running".equalsIgnoreCase(flushCurrentStatus)) {

                @SuppressLint("JobSchedulerService") ComponentName serviceComponent = new ComponentName(context, JobServiceForEvent.class);
                JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
                String alarmInterval = SharedPref.getConfigKeys(context, Config.KEYNAME_SERVICE_START_DELAY, DEFAULT_START_SERVICE_DELAY);
                final long serviceStartDelay = Long.parseLong(alarmInterval) * 60000;
                builder.setMinimumLatency(serviceStartDelay); // wait at least
                builder.setOverrideDeadline(serviceStartDelay);
                JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                int resultCode = jobScheduler.schedule(builder.build());
                if (resultCode == JobScheduler.RESULT_SUCCESS) {
                    SharedPref.setConfigKeys(context, KEYNAME_ALARM_FLAG, KEYNAME_ALARM_FLAG_SET);
                    Logger.i(TAG_CONFIG, "alarm(Via JobScheduler) will be triggered after " + alarmInterval + " Min ");
                }
            }
        } catch (IllegalArgumentException ie) {
            Logger.e(TAG_CONFIG, "Please add job service details is AndroidManifest.xml,\n<service " +
                    "android:name=\"com.vodafone.lib.seclibng.comms.JobServiceForEvent\"\nandroid:permission=\"android.permission.BIND_JOB_SERVICE\" />");
        }
    }

    /***
     * Remove unwanted character from class name
     *
     * @param classNametoFormat Classname to remove unwanted characters
     * @return Returns the class name .
     */
    public static String formattedClassName(String classNametoFormat) {
        if (classNametoFormat != null && classNametoFormat.contains("$")) {
            StringTokenizer tokens = new StringTokenizer(classNametoFormat, "$");
            return (tokens.hasMoreElements()) ? tokens.nextToken() : classNametoFormat;
        }
        return classNametoFormat;
    }

    /***
     * Check the value is null or empty for custom events
     *
     * @param value value to check
     * @return returns the same value if it is not null or length greater than zero.Else returns NA
     */
    public static String nullOrEmptyValueCheckForCustomEvent(String value) {
        try {
            if (value == null || value.trim().length() == 0) {
                return Config.DEFAULT_NA;
            } else {
                return value;
            }
        } catch (Exception e) {
            return Config.DEFAULT_NA;
        }
    }

}
