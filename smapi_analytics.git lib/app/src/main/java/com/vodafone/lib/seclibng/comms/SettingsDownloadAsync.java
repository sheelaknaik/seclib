package com.vodafone.lib.seclibng.comms;

import android.content.Context;
import android.os.AsyncTask;

import com.vodafone.lib.seclibng.certpinning.CertHelper;
import com.vodafone.lib.seclibng.storage.SqliteDb;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.Calendar;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;


/***
 * Async Task to download the settings JSON
 */

final class SettingsDownloadAsync extends AsyncTask<Void, Void, String> {

    private boolean validated = false;
    private Context mContext;
    private static final String TAGSECLIB = "SettingsDownload";
    private SqliteDb db;
    private HttpsURLConnection connection;

    public SettingsDownloadAsync(Context mContext) {
        this.mContext = mContext;
        db = SqliteDb.getInstance(mContext);
    }

    @Override
    protected String doInBackground(Void... voids) {
        URL url;
        connection = null;
        try {
            String httpAddr = SharedPref.getConfigKeys(mContext, Config.KEYNAME_SETTINGS_EVENT, Config.KEY_DEFAULT);
            if (httpAddr.equalsIgnoreCase(Config.KEY_DEFAULT)) {
                Logger.e(TAGSECLIB, "Settings API url not available");
                return null;
            }
            url = new URL(httpAddr);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.addRequestProperty(EventConstants.X_VF_TRACE_SOURCE, EventHeaders.getTraceSource(mContext));
            url.openConnection();
            connection.connect();
            Thread thr = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        CertHelper cert = new CertHelper(mContext);
                        validated = cert.cert(connection);
                    } catch (Exception ex) {

                        validated = false;
                    }
                }
            });
            thr.start();
            thr.join();
            if (validated) {
                int responseCode = connection.getResponseCode();
                Logger.i(TAGSECLIB, "Response Code for Settings URL " + responseCode);
                return Config.checkCode(responseCode, connection);
            } else {
                Logger.i(TAGSECLIB, "Certificate not validated");
                return null;
            }
        } catch (ProtocolException pe) {
            Logger.e(TAGSECLIB, "Exception occurred on SettingsDownload " + pe.getMessage());
        } catch (Exception e) {
            Logger.e(TAGSECLIB, "Exception occurred on SettingsDownload", e);
        } finally {
            if (connection != null)
                connection.disconnect();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        try {
            if (s != null) {
                Logger.i(TAGSECLIB, "Settings fetched from Settings URL");
                JSONObject jlinks = new JSONObject(s);
                for (Iterator<String> jsonKeys = jlinks.keys(); jsonKeys.hasNext(); ) {
                    String key = jsonKeys.next();
                    SharedPref.setConfigKeys(mContext, key, jlinks.getString(key));

                }
                db.checkDbAfterSettings();
                Calendar calendar = Calendar.getInstance();
                long currentDate = calendar.getTimeInMillis();
                SharedPref.setConfigKeys(mContext, Config.KEYNAME_START_DATE_FOR_SETTINGS_7_DAYS, Long.toString(currentDate));
                //TODO resets the no of resets received due to environment change or user id change.Disabled temporarily
                // Config.updateResetCountToZero(mContext);
                SharedPref.updateCurrentEventSentCount(0, mContext, true);
                SharedPref.updateExceptionCount(mContext, true);
                SharedPref.setConfigKeys(mContext, Config.KEYNAME_SETTINGS_FLAG, Config.KEYNAME_SETTINGS_RESET);
            } else {
                Logger.i(TAGSECLIB, "NO Data received from SettingsDownload");
            }

        } catch (JSONException e) {
            Logger.e("SecLibNG", "JSON Exception " + e.getMessage(), e);
        } catch (Exception e) {
            Logger.e(TAGSECLIB, e.getMessage());
        } finally {
            SharedPref.setConfigKeys(mContext, Config.KEYNAME_ENTRY_POINT_SERVICE_RUNNING_STATUS, "finished");
        }
    }
}