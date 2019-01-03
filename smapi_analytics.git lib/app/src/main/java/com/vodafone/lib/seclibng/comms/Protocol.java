package com.vodafone.lib.seclibng.comms;

import android.annotation.SuppressLint;
import android.content.Context;

import com.vodafone.lib.seclibng.Event;
import com.vodafone.lib.seclibng.certpinning.CertHelper;
import com.vodafone.lib.seclibng.storage.SqliteDb;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Scanner;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

/**
 * This Class is responsible for sending events, adding header to the entry points and other types of exception handling while sending the events.
 */
public class Protocol {
    private String idsTo = "";
    private Context context;
    private SqliteDb db;
    private static final String TAGPROTOCOL = "ProtocolTag";
    private HttpsURLConnection connection = null;
    private boolean validated = false;
    private int currentSize;
    private long timeBeforeSending;
    private boolean noUserIdEvents;
    private long currentTime;

    /***
     * initialize Protocol Class
     *
     * @param context Context
     */
    public Protocol(Context context, boolean noUserIdEvents, long currentTime) {
        this.context = context;
        db = SqliteDb.getInstance(this.context);
        this.noUserIdEvents = noUserIdEvents;
        this.currentTime = currentTime;
    }

    /***
     * Send events to server
     *
     * @param events event data
     * @throws ProtocolException exception
     */

    public void sendEvents(Event[] events) throws ProtocolException {
        String protocolSendEvents = "protocolSendEvents";
        if (events == null || events.length == 0) {
            Logger.v(protocolSendEvents, "Events are null or 0-length. Skipping to push.");
            throw new ProtocolException("No events to be sent");
        }
        JSONArray json = new JSONArray();
        for (Event event : events) {
            if (event != null) {
                json.put(event.getJSONObject());
            }
        }
        byte[] jsonBytes;
        try {
            idsTo = events[events.length - 1].getEventId();
            currentSize = events.length;
            String jsonStr = json.toString();
            Logger.i("Protocol.sendEvents", "Sending JSON: " + jsonStr);
            jsonBytes = jsonStr.getBytes("UTF-8");
            if (jsonBytes == null || jsonBytes.length == 0) {
                Logger.i(protocolSendEvents, "Bytes for JSON events resulted in null or 0 bytes. Skipping sending.");
                return;
            }
        } catch (UnsupportedEncodingException uee) {
            Logger.e(protocolSendEvents, "Unsupported encoding.", uee);
            throw new ProtocolException("Unsupported encoding while sending events "+uee.getMessage());
        } catch (RuntimeException rt) {
            Logger.e(protocolSendEvents, "Run time exception.", rt);
            throw new ProtocolException("Runtime exception while sending events "+rt.getMessage());
        }
        try {
            String servAddress = SharedPref.getConfigKeys(context, Config.KEYNAME_SUBMIT_EVENTS, "");
            if (servAddress == null || servAddress.isEmpty()) {
                throw new ProtocolException("URL is null to submit events ");
            }
            URL url = new URL(SharedPref.getConfigKeys(context, Config.KEYNAME_SUBMIT_EVENTS, ""));
            timeBeforeSending = Calendar.getInstance().getTimeInMillis();
            connection = (HttpsURLConnection) url.openConnection();
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);

            applyDefaultHeadersToConnection(connection);
            byte[] gZipjsonBytes = gzip(jsonBytes);
            if (gZipjsonBytes == null) {
                Logger.e(TAGPROTOCOL, "Unable to compress events using GZIP");
                throw new ProtocolException("Unable to compress events using GZIP");
            }

            connection.setFixedLengthStreamingMode(gZipjsonBytes.length);
            OutputStream os = connection.getOutputStream();
            Thread thr = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        CertHelper cert = new CertHelper(context);
                        validated = cert.cert(connection);
                    } catch (Exception ex) {
                        Logger.e(TAGPROTOCOL, "Cert Pinning Exception " + ex.getMessage(), ex);
                        validated = false;
                    }
                }
            });

            thr.start();
            thr.join();

            if (validated) {
                os.write(gZipjsonBytes);
                os.flush();
                os.close();

            } else {
                Logger.i(TAGPROTOCOL, "Certificate not validated");
                throw new ProtocolException("Certificate not validated");
            }
            int responseCode = connection.getResponseCode();
            checkResponseCode(responseCode, connection);
        } catch (MalformedURLException mfue) {
            Logger.e(protocolSendEvents, "Malformed URL " + mfue.getMessage());
           // throw new ProtocolException("Malformed URL "+mfue.getMessage());
        } catch (ProtocolException spe) {
            Logger.e(protocolSendEvents, "Connection Issue " + spe.getMessage(), spe);
        } catch (Exception e) {
            Logger.e(TAGPROTOCOL, "Exception while sending events " + e.getMessage(), e);
          //  throw new ProtocolException("Exception while sending events " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    @SuppressLint("LongLogTag")
    private void checkResponseCode(int responseCode, HttpURLConnection connection) throws ProtocolException {

        long timeDiff;

        final String TAG = "Protocol.checkResponseCode";

        if (responseCode >= 200 && responseCode <= 399) {
            timeDiff = Calendar.getInstance().getTimeInMillis() - timeBeforeSending;
            Logger.v(TAG, "Response code " + responseCode + " is good,");
            Logger.v(TAG, timeDiff + " ms took to send " + currentSize + " Events");
            if (noUserIdEvents) {
                db.deleteEventsNoUserIdEvents(currentTime);
            } else {
                db.deleteEvents(idsTo, false);
            }
            SharedPref.updateCurrentEventSentCount(currentSize, context, false);
            //noinspection UnnecessaryReturnStatement
            return;
        }
        else
        {
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
            }catch (IOException pe)
            {
                throw new ProtocolException("Error " + pe.getMessage());
            }
        }
    }
    /***
     * Header for the submit event service
     *
     * @param connection Http url connection object
     */
    private void applyDefaultHeadersToConnection(HttpURLConnection connection) {
        connection.addRequestProperty(EventConstants.CONNECTION, "Close");
        connection.addRequestProperty(EventConstants.CONTENT_TYPE, EventHeaders.getContentType());
        connection.addRequestProperty(EventConstants.OS_VERSION, EventHeaders.getOsVersion());
        connection.addRequestProperty(EventConstants.SECLIB_CLIENT_VERSION, EventHeaders.getClientVersion());
        connection.addRequestProperty(EventConstants.X_VF_TRACE_DEVICE_ID, EventHeaders.getSubjectId(context));
        connection.addRequestProperty(EventConstants.X_VF_SCREEN_HEIGHT, EventHeaders.getHeight(context));
        connection.addRequestProperty(EventConstants.X_VF_SCREEN_WIDTH, EventHeaders.getWidth(context));
        connection.addRequestProperty(EventConstants.X_VF_TRACE_SOURCE_VERSION, EventHeaders.getTraceSourceVersion(context));
        connection.addRequestProperty(EventConstants.INSTALL_ID, SharedPref.getInstallId(context));
        connection.addRequestProperty(EventConstants.X_VF_TRACE_APPLICATION_NAME, EventHeaders.getApplicationName(context));
        connection.addRequestProperty(EventConstants.X_VF_TRACE_SUBJECT_ID, EventHeaders.getSubjectId(context));
        connection.addRequestProperty(EventConstants.ACCEPT_ENCODING, EventConstants.CONTENT_GZIP);
        connection.setRequestProperty(EventConstants.CONTENT_ENCODING, EventConstants.CONTENT_GZIP);
        connection.addRequestProperty(EventConstants.X_VF_TRACE_OS_NAME, EventHeaders.getOsName());
        connection.addRequestProperty(EventConstants.X_VF_TRACE_NETWORK_BEARER, EventHeaders.getNetWorkBearer(context));
        connection.addRequestProperty(EventConstants.X_VF_TRACE_MCC, EventHeaders.getMcc(context));
        connection.addRequestProperty(EventConstants.X_VF_TRACE_MNC, EventHeaders.getMnc(context));
        connection.addRequestProperty(EventConstants.X_VF_TRACE_SUBJECT_REGION, EventHeaders.getSubjectRegion(context));
        connection.addRequestProperty(EventConstants.X_VF_TRACE_LOCALE, EventHeaders.getLocale());
        connection.addRequestProperty(EventConstants.X_VF_TRACE_SOURCE, EventHeaders.getTraceSource(context));
        connection.addRequestProperty(EventConstants.X_VF_TRACE_USER_AGENT, EventHeaders.getUserAgent());
        connection.addRequestProperty(EventConstants.X_VF_TRACE_PLATFORM, EventHeaders.getPlatform());
    }
    /***
     * Compress the json byte array nd returns the compressed byte array
     *
     * @param val Json Byte Array
     * @return Compressed byte array
     * @throws IOException exception
     */
    private static byte[] gzip(byte[] val)  throws IOException{
        byte[] result=null;
        GZIPOutputStream gos = null;
        ByteArrayOutputStream bos =null;
        try {
            bos = new ByteArrayOutputStream(val.length);

            gos = new GZIPOutputStream(bos);
            gos.write(val, 0, val.length);

            gos.flush();
            gos.finish();

            result = bos.toByteArray();
        }
        finally {
            try {
                    if (gos != null & bos != null)
                    {
                        gos.close();
                        bos.close();
                    }
                } catch (IOException ie) {
                    Logger.e(TAGPROTOCOL, "IO exception while closing the GZIP");
                }
        }
        return result;
    }
}