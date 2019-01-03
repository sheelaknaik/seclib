package com.vodafone.lib.seclibng;

import android.text.TextUtils;

import com.vodafone.lib.seclibng.comms.Config;
import com.vodafone.lib.seclibng.comms.EventConstants;
import com.vodafone.lib.seclibng.comms.Logger;
import com.vodafone.lib.seclibng.comms.SharedPref;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;


@Aspect
public class NetworkAspect {
    private static final String LOG_TAG = "NetworkAspect";
    @Pointcut("(call(* java.net.URL.openConnection()) && (!within(com.vodafone.lib.seclibng.*..*)))")
    public void onOpenConnection(JoinPoint joinPoint) {
        // afterOpenConnection()
    }

    @After("onOpenConnection(*)")
    public void afterOpenConnection(final JoinPoint joinPoint) throws Exception {
        System.out.println(LOG_TAG + "====" + "onOpenConnection");
        if (joinPoint.getTarget() instanceof URL) {
            try {
                URL url = (URL) joinPoint.getTarget();
                if (url != null) {
                    final long connectionId = System.identityHashCode(url);

                    NetworkCallRepo.getSharedInstance().setUrl(connectionId, url);
                    NetworkCallRepo.getSharedInstance().setRequestStartTime(connectionId, System.currentTimeMillis());

                    Event event = NetworkCallRepo.getSharedInstance().getNetworkEvent(connectionId);

                    if (event != null && event.getEventDescription() != null) {
                        JSONObject descriptionJson = new JSONObject(event.getEventDescription());
                        descriptionJson.put(EventConstants.X_ENDPOINT, url.toString());
                        descriptionJson.put(EventConstants.X_PROTOCOL, url.getProtocol());

                        event.addPayload(EventConstants.EVENT_DESCRIPTION, descriptionJson.toString());
                        NetworkCallRepo.getSharedInstance().setNetworkEvent(connectionId, event);
                    }
                }

            } catch (Exception e) {
                Logger.i(NetworkAspect.class.getName(), e.getMessage());
            }
        }
    }


    @Pointcut("(call(void java.net.URLConnection.connect()) && (!within(com.vodafone.lib.seclibng.*..*)))")
    public void onConnect(JoinPoint joinPoint) {
        // beforeConnect()
    }

    @Before("onConnect(*)")
    public void beforeConnect(final JoinPoint joinPoint) throws Exception {
        System.out.println(LOG_TAG + "====" + "onConnect");
        if (joinPoint.getTarget() instanceof URLConnection) {
            try {
                URLConnection connection = (URLConnection) joinPoint.getTarget();

                if (connection != null && connection.getURL() != null) {
                    final URL url = connection.getURL();
                    final long connectionId = System.identityHashCode(url);
                    NetworkCallRepo.getSharedInstance().setUrl(connectionId, url);
                    Event event = NetworkCallRepo.getSharedInstance().getNetworkEvent(connectionId);

                    if (event != null && event.getEventDescription() != null) {
                        JSONObject descriptionJson = new JSONObject(event.getEventDescription());

                        Set<String> keys = connection.getRequestProperties().keySet();
                        String requestContentType = Config.DEFAULT_NA;
                        JSONObject requestHeaders = new JSONObject();

                        if (!keys.isEmpty()) {
                            for (String key : keys) {
                                if (!TextUtils.isEmpty(key)) {
                                    requestHeaders.put(key, connection.getRequestProperty(key));
                                    if (key.equalsIgnoreCase("content-type")) {
                                        requestContentType = connection.getRequestProperty(key);
                                    }
                                }
                            }
                        }

                        descriptionJson.put(EventConstants.X_REQUEST_DATE, new Date(System.currentTimeMillis()));
                        descriptionJson.put(EventConstants.X_ENDPOINT, url.toString());
                        descriptionJson.put(EventConstants.X_PROTOCOL, url.getProtocol());
                        descriptionJson.put(EventConstants.X_REQ_HEADER, new JSONArray().put(requestHeaders));

                        if (!requestContentType.equals(Config.DEFAULT_NA)) {
                            descriptionJson.put(EventConstants.X_REQ_CONTENTTYPE, requestContentType);
                        }

                        event.addPayload(EventConstants.EVENT_DESCRIPTION, descriptionJson.toString());
                        event.addPayload(EventConstants.EVENT_ELEMENT, getRequestMethod(connection));
                        NetworkCallRepo.getSharedInstance().setNetworkEvent(connectionId, event);
                    }
                }
            } catch (Exception e) {
                Logger.i(NetworkAspect.class.getName(), e.getMessage());
            }
        }
    }

    @Pointcut("(call(* java.net.URLConnection.getInputStream()) && (!within(com.vodafone.lib.seclibng.*..*)))")
    public void onGetInputStream(JoinPoint joinPoint) {
        // afterGetInputStream()
    }

    @After("onGetInputStream(*)")
    public void afterGetInputStream(JoinPoint joinPoint) throws Exception {
        System.out.println(LOG_TAG + "====" + "onGetInputStream");
        if (joinPoint.getTarget() instanceof URLConnection) {
            try {

                URLConnection connection = (URLConnection) joinPoint.getTarget();

                if (connection != null && connection.getURL() != null) {
                    final URL url = connection.getURL();
                    final long connectionId = System.identityHashCode(url);

                    Event event = NetworkCallRepo.getSharedInstance().getNetworkEvent(connectionId);

                    if (event != null && event.getEventDescription() != null) {
                        Set<String> keys = connection.getHeaderFields().keySet();
                        JSONObject headers = new JSONObject();

                        if (!keys.isEmpty()) {
                            for (String key : keys) {
                                if (!TextUtils.isEmpty(key)) {
                                    headers.put(key, connection.getHeaderField(key));
                                }
                            }
                        }

                        JSONObject descriptionJson = new JSONObject(event.getEventDescription());
                        descriptionJson.put(EventConstants.X_RES_HEADER, new JSONArray().put(headers));
                        descriptionJson.put(EventConstants.X_ENDPOINT, url.toString());
                        descriptionJson.put(EventConstants.X_PROTOCOL, url.getProtocol());
                        descriptionJson.put(EventConstants.X_RES_CONTENTYPE, connection.getContentType());
                        descriptionJson.put(EventConstants.X_RESPONSE_CODE, getResponseCode(connection));

                        if (isErrorResponseCode(connection)) {
                            descriptionJson.put(EventConstants.X_ERROR, getResponseMessage(connection));
                        } else {
                            descriptionJson.put(EventConstants.X_RESPONSE_MSG, getResponseMessage(connection));
                        }

                        if (NetworkCallRepo.getSharedInstance().getRequestStartTime(connectionId) != null) {
                            final long responseTime = System.currentTimeMillis() - NetworkCallRepo.getSharedInstance().getRequestStartTime(connectionId);
                            descriptionJson.put(EventConstants.X_RESPONSE_TIME, responseTime);
                        }

                        event.addPayload(EventConstants.EVENT_DESCRIPTION, descriptionJson.toString());
                        SecLibNG.getInstance().logNetworkevent(getRequestMethod(connection), event.getEventDescription(), event.getEventType(), getTraceIdValue(connection));
                        NetworkCallRepo.getSharedInstance().removeURL(connectionId);
                    }
                }
            } catch (Exception e) {
                Logger.i(NetworkAspect.class.getName(), e.getMessage());
            }
        }
    }

    @Pointcut("(call(* java.net.URLConnection.getOutputStream(..)) && (!within(com.vodafone.lib.seclibng.*..*)))")
    public void onGetOutputStream(JoinPoint joinPoint) {
        // afterGetErrorStream()
    }

    @After("onGetOutputStream(*)")
    public void afterGetErrorStream(JoinPoint joinPoint) {
        System.out.println(LOG_TAG + "====" + "onGetOutputStream"+joinPoint.getTarget().toString());
        if (joinPoint.getTarget() instanceof URLConnection) {
            try {

                URLConnection connection = (URLConnection) joinPoint.getTarget();

                if (connection != null && connection.getURL() != null) {
                    final URL url = connection.getURL();
                    final long connectionId = System.identityHashCode(url);

                    Event event = NetworkCallRepo.getSharedInstance().getNetworkEvent(connectionId);

                    if (event != null && event.getEventDescription() != null) {
                        Set<String> keys = connection.getHeaderFields().keySet();
                        JSONObject headers = new JSONObject();

                        if (!keys.isEmpty()) {
                            for (String key : keys) {
                                if (!TextUtils.isEmpty(key)) {
                                    headers.put(key, connection.getHeaderField(key));
                                }
                            }
                        }

                        JSONObject descriptionJson = new JSONObject(event.getEventDescription());
                        descriptionJson.put(EventConstants.X_RES_HEADER, new JSONArray().put(headers));
                        descriptionJson.put(EventConstants.X_ENDPOINT, url.toString());
                        descriptionJson.put(EventConstants.X_PROTOCOL, url.getProtocol());
                        descriptionJson.put(EventConstants.X_RES_CONTENTYPE, connection.getContentType());
                        descriptionJson.put(EventConstants.X_RESPONSE_CODE, getResponseCode(connection));

                        if (isErrorResponseCode(connection)) {
                            descriptionJson.put(EventConstants.X_ERROR, getResponseMessage(connection));
                        } else {
                            descriptionJson.put(EventConstants.X_RESPONSE_MSG, getResponseMessage(connection));
                        }

                        if (NetworkCallRepo.getSharedInstance().getRequestStartTime(connectionId) != null) {
                            final long responseTime = System.currentTimeMillis() - NetworkCallRepo.getSharedInstance().getRequestStartTime(connectionId);
                            descriptionJson.put(EventConstants.X_RESPONSE_TIME, responseTime);
                        }

                        event.addPayload(EventConstants.EVENT_DESCRIPTION, descriptionJson.toString());
                        SecLibNG.getInstance().logNetworkevent(getRequestMethod(connection), event.getEventDescription(), event.getEventType(), getTraceIdValue(connection));
                        NetworkCallRepo.getSharedInstance().removeURL(connectionId);
                    }
                }
            } catch (Exception e) {
                Logger.i(NetworkAspect.class.getName(), e.getMessage());
            }
        }
    }


    @Pointcut("((call(* java.net.HttpURLConnection.getErrorStream(..)) || (call(* java.net.HttpURLConnection.getResponseCode()) || (call(* java.net.HttpURLConnection.getResponseMessage()) || (call(* javax.net.ssl.HttpsURLConnection.getLocalCertificates(..)) || (call(* javax.net.ssl.HttpsURLConnection.getCipherSuite(..)) || (call(* javax.net.ssl.HttpsURLConnection.getPeerPrincipal(..)) || (call(* javax.net.ssl.HttpsURLConnection.getLocalPrincipal(..)) || (call(* java.net.URLConnection.connect(..)) || (call(* java.net.URLConnection.getContent(..)) || (call(* java.net.URLConnection.getContentEncoding(..)) || (call(* java.net.URLConnection.getContentLength(..)) || (call(* java.net.URLConnection.getContentType(..)) || (call(* java.net.URLConnection.getDate(..)) || (call(* java.net.URLConnection.getExpiration(..)) || (call(* java.net.URLConnection.getHeaderField(..)) || (call(* java.net.URLConnection.getHeaderFieldDate(..)) || (call(* java.net.URLConnection.getHeaderFieldKey(..)) || (call(* java.net.URLConnection.getHeaderFields(..)) || (call(* java.net.URLConnection.getInputStream()) || call(* java.net.URLConnection.getLastModified(..))))))))))))))))))))) && !within(com.vodafone.lib.*..*))")
    public void onResponseAccess(JoinPoint joinPoint) {
        // afterResponseAccess()
    }

    @Before("onResponseAccess(*)")
    public void afterResponseAccess(JoinPoint joinPoint) throws Exception {
        System.out.println(LOG_TAG + "====" + "onResponseAccess");
        if (joinPoint.getTarget() instanceof URLConnection) {
            try {
                URLConnection connection = (URLConnection) joinPoint.getTarget();

                if (connection != null && connection.getURL() != null) {
                    final URL url = connection.getURL();
                    final long connectionId = System.identityHashCode(url);
                    Event event = NetworkCallRepo.getSharedInstance().getNetworkEvent(connectionId);

                    if (event != null && event.getEventDescription() != null) {
                        Set<String> keys = connection.getHeaderFields().keySet();
                        JSONObject headers = new JSONObject();

                        if (!keys.isEmpty()) {
                            for (String key : keys) {
                                if (!(key == null || key.isEmpty())) {
                                    headers.put(key, connection.getHeaderField(key));
                                }
                            }
                        }

                        JSONObject descriptionJson = new JSONObject(event.getEventDescription());
                        descriptionJson.put(EventConstants.X_ENDPOINT, url.toString());
                        descriptionJson.put(EventConstants.X_PROTOCOL, url.getProtocol());
                        descriptionJson.put(EventConstants.X_RES_HEADER, new JSONArray().put(headers));
                        descriptionJson.put(EventConstants.X_RES_CONTENTYPE, connection.getContentType());

                        if (isErrorResponseCode(connection)) {
                            descriptionJson.put(EventConstants.X_ERROR, getResponseMessage(connection));
                        } else {
                            descriptionJson.put(EventConstants.X_RESPONSE_MSG, getResponseMessage(connection));
                        }
                        descriptionJson.put(EventConstants.X_RESPONSE_CODE, getResponseCode(connection));

                        if (NetworkCallRepo.getSharedInstance().getRequestStartTime(connectionId) != null) {
                            final long responseTime = System.currentTimeMillis() - NetworkCallRepo.getSharedInstance().getRequestStartTime(connectionId);
                            descriptionJson.put(EventConstants.X_RESPONSE_TIME, responseTime);
                        }

                        event.addPayload(EventConstants.EVENT_DESCRIPTION, descriptionJson.toString());
                        SecLibNG.getInstance().logNetworkevent(getRequestMethod(connection), event.getEventDescription(), event.getEventType(), getTraceIdValue(connection));
                        NetworkCallRepo.getSharedInstance().removeURL(connectionId);
                    }
                }
            } catch (Exception e) {
                Logger.i(NetworkAspect.class.getName(), e.getMessage());
            }
        }
    }


    private String getTraceIdValue(URLConnection connection) {
        String traceValue = Config.DEFAULT_NA;
        if (SecLibNG.getInstance().getAppContext() != null) {
            String traceIdKey = SharedPref.getConfigKeys(SecLibNG.getInstance().getAppContext(), Config.KEYNAME_TRACE_ID, Config.KEYNAME_TRACE_ID_DEFAULT);

            if (!(connection.getRequestProperty(traceIdKey) == null || connection.getRequestProperty(traceIdKey).isEmpty())) {
                traceValue = connection.getRequestProperty(traceIdKey);
            }
        }

        return traceValue;
    }

    private String getRequestMethod(URLConnection connection) {
        if (connection != null) {
            if (connection instanceof HttpsURLConnection) {
                return ((HttpsURLConnection) connection).getRequestMethod();
            } else if (connection instanceof HttpURLConnection) {
                return ((HttpURLConnection) connection).getRequestMethod();
            }
        }
        return Config.DEFAULT_NA;
    }


    private String getResponseCode(URLConnection connection) throws IOException {
        if (connection != null) {
            if (connection instanceof HttpsURLConnection) {
                return String.valueOf(((HttpsURLConnection) connection).getResponseCode());
            } else if (connection instanceof HttpURLConnection) {
                return String.valueOf(((HttpURLConnection) connection).getResponseCode());
            }
        }
        return Config.DEFAULT_NA;
    }

    private String getResponseMessage(URLConnection connection) throws IOException {
        if (connection instanceof HttpsURLConnection) {
            return ((HttpsURLConnection) connection).getResponseMessage();
        } else if (connection instanceof HttpURLConnection) {
            return ((HttpURLConnection) connection).getResponseMessage();
        }
        return Config.DEFAULT_NA;
    }

    private boolean isErrorResponseCode(URLConnection connection) throws IOException {
        String responseCode = getResponseCode(connection);
        if (!responseCode.equalsIgnoreCase(Config.DEFAULT_NA)) {
            int code = Integer.parseInt(responseCode);
            return (code < 100 || code >= 400);
        }
        return true;
    }
}
