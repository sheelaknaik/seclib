package com.vodafone.lib.seclibng.comms;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

import com.vodafone.lib.seclibng.SecLibNG;
import com.vodafone.lib.seclibng.internal.EventsIntentService;
import com.vodafone.lib.seclibng.storage.SqliteDb;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

/**
 * Service to retrieve Home Doc if it fails to receive any data from app starting
 */
public class EntryPointHandling extends JobIntentService {

    private static final String TAGSECLIB = "EntryPointHandling";
    private static final int UNIQUE_JOB_ID=1001;

    @Override
    protected void onHandleWork(@NonNull Intent intent) {

        Logger.i(TAGSECLIB, "EntryPointHandling called");

        SharedPref.setConfigKeys(getApplicationContext(),Config.KEYNAME_ENTRY_POINT_SERVICE_RUNNING_STATUS,"running");
        String action = intent.getStringExtra("action");

        if (action.equalsIgnoreCase(Config.HOME_DOC)){
            String homeDocRunningStatus = SharedPref.getConfigKeys(getApplicationContext(), Config.KEYNAME_HOME_DOC_RUNNING, Config.KEYNAME_HOME_DOC_RUNNING_NO);
            if (!homeDocRunningStatus.equalsIgnoreCase(Config.KEYNAME_HOME_DOC_RUNNING_YES))
                new EntryPointHandling.LoadHomeDoc().execute();
        } else if (action.equalsIgnoreCase(Config.DELETE_DATA)) {
            deleteOldData();
            SharedPref.setConfigKeys(getApplicationContext(),Config.KEYNAME_ENTRY_POINT_SERVICE_RUNNING_STATUS,"finished");
        } else {
            SettingsDownloadAsync settingsDownloadClass = new SettingsDownloadAsync(getApplicationContext());
            settingsDownloadClass.execute();
        }
    }

    public static void enqueueWork(Context ctxt, Intent i) {
        enqueueWork(ctxt, EntryPointHandling.class, UNIQUE_JOB_ID, i);
    }
/*

    *//***
     * Starts fetching HomeDoc
     */
    private class LoadHomeDoc extends AsyncTask<Void, Void, String[]> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            SharedPref.setConfigKeys(getApplicationContext(), Config.KEYNAME_HOME_DOC_RUNNING, Config.KEYNAME_HOME_DOC_RUNNING_YES);
        }

        @Override
        protected String[] doInBackground(Void... voids) {
            URL url;
            HttpsURLConnection connection = null;
            try {
                if (SecLibNG.getInstance().getEnvironment().equalsIgnoreCase(SecLibNG.ENVIRONMENT_PRE))
                    url = new URL(EventConstants.ENV_PRE_HOMEDOC_URL);
                else
                    url = new URL(EventConstants.ENV_PRO_HOMEDOC_URL);
                connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                connection.addRequestProperty(EventConstants.X_VF_TRACE_SOURCE, "android:" + EventHeaders.getTraceSource(getApplicationContext()));
                url.openConnection();
                connection.connect();
                int responseCode = connection.getResponseCode();
                Logger.i(TAGSECLIB, "Response Code for HomeDoc " + responseCode);
                String certificates = connection.getHeaderField(Config.HEADDER_STRING);
                if (certificates == null || certificates.isEmpty()) {
                    Logger.i(TAGSECLIB, "No certificates received from Home doc");
                    return null;
                }
                return new String[]{Config.checkCode(responseCode, connection), certificates};
            } catch (ProtocolException pe) {
                Logger.e(TAGSECLIB, "Protocol exception occurred on HomeDoc retrieval: " + pe.getMessage(), pe);
            } catch (Exception e) {
                Logger.e(TAGSECLIB, "Exception occurred on HomeDoc retrieval: " + e.getMessage(), e);
            } finally {
                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] s) {
            super.onPostExecute(s);
            try {
                SharedPref.setConfigKeys(getApplicationContext(), Config.KEYNAME_HOME_DOC_RUNNING, Config.KEYNAME_HOME_DOC_RUNNING_NO);
                if (s != null) {
                    Logger.i(TAGSECLIB, "Entry point fetched from Home doc");
                    SharedPref.setConfigKeys(getApplicationContext(), Config.KEYNAME_HOME_DOC_RETRIEVED, Config.KEYNAME_HOME_DOC_RETRIEVED_YES);
                    JSONObject jobmain = new JSONObject(s[0]);
                    JSONObject jlinks = jobmain.getJSONObject("links");
                    for (Iterator<String> jsonKeys = jlinks.keys(); jsonKeys.hasNext(); ) {
                        String key = jsonKeys.next();
                        SharedPref.setConfigKeys(getApplicationContext(), key, jlinks.getJSONObject(key).getString("href"));
                    }
                    String[] keys = s[1].split(";");
                    Set<String> keyList = new HashSet<>();
                    String shaString;
                    String key;
                    for (String key1 : keys) {
                        if (key1.trim().startsWith("pin-sha256")) {
                            shaString = key1;
                            key = shaString.replace("pin-sha256=", "").replace("\"", "").trim();
                            keyList.add(key);
                        }
                    }
                    SharedPref.setKeys(keyList, getApplicationContext());
                }

                deleteOldData();

            } catch (JSONException e) {
                Logger.e(TAGSECLIB, "JSON Exception occurred: " + e.getMessage(), e);
            } catch (Exception e) {
                Logger.e(TAGSECLIB, "JSON exception on fetching HomeDoc: " + e.getMessage(), e);
            }
        }
    }

    /***
     * Checks the date whether if > 7 days to reset max event counter .
     */
    private void checkSettings() {
        long lastcheckDate = Long.parseLong(SharedPref.getConfigKeys(getApplicationContext(), Config.KEYNAME_START_DATE_FOR_SETTINGS_7_DAYS, "0"));
        Calendar calendar = Calendar.getInstance();
        long currentDate = calendar.getTimeInMillis();
        SettingsDownloadAsync settingsDownloadClass = new SettingsDownloadAsync(getApplicationContext());
        if (lastcheckDate == 0) {
            SharedPref.setConfigKeys(getApplicationContext(), Config.KEYNAME_START_DATE_FOR_SETTINGS_7_DAYS, Long.toString(currentDate));
            if (Config.isOnline(getApplicationContext())) {
                settingsDownloadClass.execute();
            } else {
                Logger.i(TAGSECLIB, "Internet Connection not available");
                SharedPref.setConfigKeys(getApplicationContext(), Config.KEYNAME_SETTINGS_FLAG, Config.KEYNAME_SETTINGS_SET);
            }
        } else if (SharedPref.getConfigKeys(getApplicationContext(), Config.KEYNAME_SETTINGS_FLAG, Config.KEYNAME_SETTINGS_SET).equalsIgnoreCase(Config.KEYNAME_SETTINGS_SET)) {
            if (Config.isOnline(getApplicationContext())) {
                settingsDownloadClass.execute();
            }
        } else {
            long diff = currentDate - lastcheckDate;
            long days = diff / (24 * 60 * 60 * 1000);
            if (days > 6) {
                if (Config.isOnline(getApplicationContext())) {
                    Logger.i(TAGSECLIB, "Time to reset the counter and flush the data to backend");
                    Config.KEYNAME_RESEND_EVENTS = true;
                    settingsDownloadClass.execute();
                } else {
                    Logger.i(TAGSECLIB, "Internet connection is not available.");
                    SharedPref.setConfigKeys(getApplicationContext(), Config.KEYNAME_SETTINGS_FLAG, Config.KEYNAME_SETTINGS_SET);
                }
            }
        }
    }

    /***
     * Delete old data from database on first time while opening the app
     */
    private void deleteOldData() {
        final SqliteDb db = SqliteDb.getInstance(getApplicationContext());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                db.deleteOldData();
            }
        };
        Thread thread = new Thread(runnable);
        Calendar cToday = Calendar.getInstance();
        String currentDate = cToday.get(Calendar.DAY_OF_MONTH) + "-" + cToday.get(Calendar.MONTH);
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(SharedPref.SHARED_PREFERENCE_NAME, 0);

        if (!sharedPreferences.contains(Config.KEYNAME_DB_LAST_CHECKED_DATE) || !SharedPref.getConfigKeys(getApplicationContext(), Config.KEYNAME_DB_LAST_CHECKED_DATE, currentDate).equalsIgnoreCase(currentDate)) {
            if (sharedPreferences.contains(Config.KEYNAME_DB_LAST_CHECKED_DATE)) {
                Intent intent = new Intent(getApplicationContext(), EventsIntentService.class).putExtra("action", Config.NO_USER_ID_DATA);
                EventsIntentService.enqueueWork(getApplicationContext(), intent);
            }
            checkSettings();
            thread.start();
        } else {
            if (SharedPref.getConfigKeys(getApplicationContext(), Config.KEYNAME_SETTINGS_FLAG, Config.KEYNAME_SETTINGS_SET).equalsIgnoreCase(Config.KEYNAME_SETTINGS_SET) && Config.isOnline(getApplicationContext())) {
                SettingsDownloadAsync settingsDownloadClass = new SettingsDownloadAsync(getApplicationContext());
                settingsDownloadClass.execute();
            }
        }
    }

   /* @Override
    public void onDestroy() {
        super.onDestroy();
        Config.setConfigKeys(getApplicationContext(),Config.KEYNAME_ENTRY_POINT_SERVICE_RUNNING_STATUS,"finished");
    }*/
}
