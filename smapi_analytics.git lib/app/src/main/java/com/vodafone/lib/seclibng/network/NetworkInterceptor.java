/*
 * Copyright (C) 2015 Square, Inc, 2017.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vodafone.lib.seclibng.network;

import android.content.Context;
import android.util.Log;

import com.vodafone.lib.seclibng.Event;
import com.vodafone.lib.seclibng.SecLibNG;
import com.vodafone.lib.seclibng.comms.Config;
import com.vodafone.lib.seclibng.comms.EventConstants;
import com.vodafone.lib.seclibng.comms.Logger;
import com.vodafone.lib.seclibng.comms.SharedPref;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okio.Buffer;
import okio.BufferedSource;
import okio.GzipSource;
import okio.Okio;

/**
 * An OkHttp3 Interceptor which persists and displays HTTP activity in your application for later inspection.
 */
public final class NetworkInterceptor implements Interceptor {

    private static final String LOG_TAG = "NetworkInterceptor";
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final Context context;
    private long maxContentLength = 250000L;

    /**
     * @param context The current Context.
     */
    public NetworkInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Set the maximum length for request and response content before it is truncated.
     * Warning: setting this value too high may cause unexpected results.
     *
     * @param max the maximum length (in bytes) for request/response content.
     * @return The {@link NetworkInterceptor} instance.
     */
    public NetworkInterceptor maxContentLength(long max) {
        this.maxContentLength = max;
        return this;
    }

    /**
     * method to intercept network calls
     * @param chain chain
     * @return HTTP response
     * @throws IOException exception
     */
    @Override public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();
        int count =0;
        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;

        HttpTransaction transaction = new HttpTransaction();

        transaction.setRequestDate(new Date());

        transaction.setMethod(request.method());
        transaction.setUrl(request.url().toString());

        transaction.setRequestHeaders(request.headers());

        String traceId= verifyTraceHeader(request.headers());
        transaction.setTraceId(traceId);

        if (hasRequestBody) {
            if (requestBody.contentType() != null) {
                transaction.setRequestContentType(requestBody.contentType().toString());
            }
            if (requestBody.contentLength() != -1) {
                transaction.setRequestContentLength(requestBody.contentLength());
            }
        }

        transaction.setRequestBodyIsPlainText(!bodyHasUnsupportedEncoding(request.headers()));
        if (hasRequestBody && transaction.requestBodyIsPlainText()) {

            BufferedSource source = getNativeSource(new Buffer(), bodyGzipped(request.headers()));
            Buffer buffer = source.buffer();
            requestBody.writeTo(buffer);
            Charset charset = UTF8;
            MediaType contentType = requestBody.contentType();

            if (contentType != null) {
                charset = contentType.charset(UTF8);
            }
            if (isPlaintext(buffer)) {
                transaction.setRequestBody(readFromBuffer(buffer, charset));
            } else {
                transaction.setResponseBodyIsPlainText(false);
            }
        }

        transaction.setId(count+1);

        long startNs = System.nanoTime();
        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            transaction.setError(e.toString());

            //Create SMAPI network event
            //createNetworkEvent(transaction);
            Logger.e(LOG_TAG, "Exception during network interception " + e.getMessage());

            throw e;
        }
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        ResponseBody responseBody = response.body();

        transaction.setRequestHeaders(response.request().headers());

        transaction.setResponseDate(new Date());
        transaction.setTookMs(tookMs);
        transaction.setProtocol(response.protocol().toString());

        transaction.setResponseCode(response.code());

        transaction.setResponseMessage(response.message());

        transaction.setResponseContentLength(responseBody.contentLength());

        if (responseBody.contentType() != null) {
            transaction.setResponseContentType(responseBody.contentType().toString());
        }
        transaction.setResponseHeaders(response.headers());

        transaction.setResponseBodyIsPlainText(!bodyHasUnsupportedEncoding(response.headers()));
        if (HttpHeaders.hasBody(response) && transaction.responseBodyIsPlainText()) {
            BufferedSource source = getNativeSource(response);
            source.request(Long.MAX_VALUE);
            Buffer buffer = source.buffer();

            Charset charset = UTF8;
            MediaType contentType = responseBody.contentType();
            if (contentType != null) {
                try {
                    charset = contentType.charset(UTF8);
                } catch (UnsupportedCharsetException e) {
                    Logger.e(LOG_TAG, "UnsupportedCharsetException during network interception " + e.getMessage());

                    //Create SMAPI network event
                    createNetworkEvent(transaction);
                    return response;
                }
            }
            if (isPlaintext(buffer)) {

                transaction.setResponseBody(readFromBuffer(buffer.clone(), charset));
            } else {
                transaction.setResponseBodyIsPlainText(false);
            }
            transaction.setResponseContentLength(buffer.size());
        }

        //Create SMAPI network event
        createNetworkEvent(transaction);

        return response;
    }

    private String verifyTraceHeader(Headers headers) {
        String traceValue= Config.DEFAULT_NA;

        String traceIdKey = SharedPref.getConfigKeys(context, Config.KEYNAME_TRACE_ID, Config.KEYNAME_TRACE_ID_DEFAULT);

        int size = headers.size();
        for (int i = 0; i < size; i++) {
            String fieldName = headers.name(i);
            String value = headers.value(i);

            if(fieldName.equalsIgnoreCase(traceIdKey))
            {
                traceValue = value;
                return traceValue;
            }
        }
        return traceValue;
    }

    /**
     * Method to create network events in SMAPI
     * @param transaction input
     */
    private void createNetworkEvent(HttpTransaction transaction) {
        JSONArray responseArray = new JSONArray();
        JSONObject responseItem = new JSONObject();

        JSONArray requestArray = new JSONArray();
        JSONObject requestItem = new JSONObject();

        JSONObject responseObj = new JSONObject();
        try {
            responseObj.put(EventConstants.X_REQUEST_DATE, transaction.getRequestDate());
            responseObj.put(EventConstants.X_RESPONSE_TIME, transaction.getTookMs());
            responseObj.put(EventConstants.X_PROTOCOL,transaction.getProtocol());
            responseObj.put(EventConstants.X_ENDPOINT,transaction.getUrl());
            responseObj.put(EventConstants.X_REQ_CONTENTTYPE,transaction.getRequestContentType());
            responseObj.put(EventConstants.X_RESPONSE_CODE,transaction.getResponseCode());
            responseObj.put(EventConstants.X_RESPONSE_MSG,transaction.getResponseMessage());
            responseObj.put(EventConstants.X_ERROR,transaction.getError());
            responseObj.put(EventConstants.X_RES_CONTENTYPE,transaction.getResponseContentType());

            if(transaction.getResponseHeaders()!=null ) {
                for (HttpHeader header : transaction.getResponseHeaders()) {
                    responseItem.put(header.getName(), header.getValue());
            }
                responseArray.put(responseItem);
                responseObj.put("responseHeaders",responseArray);
            }

            if(transaction.getRequestHeaders()!=null ) {
                for (HttpHeader header : transaction.getRequestHeaders()) {
                    requestItem.put(header.getName(), header.getValue());
                }
                requestArray.put(requestItem);
                responseObj.put("requestHeaders",requestArray);
            }

            String eventElement = transaction.getMethod() ;
            SecLibNG.getInstance().logNetworkevent(eventElement,responseObj.toString(), Event.EventType.NETWORK,transaction.getTraceId());

        } catch (JSONException e) {
            Logger.e(LOG_TAG, "JSONException in network interception " + e.getMessage());

            e.printStackTrace();
        }
    }


    /**
     * Returns true if the body in question probably contains human readable text. Uses a small sample
     * of code points to detect unicode control characters commonly used in binary file signatures.
     */
    private boolean isPlaintext(Buffer buffer) {
        try {

            Buffer prefix = new Buffer();
            long byteCount = buffer.size() < 64 ? buffer.size() : 64;
            buffer.copyTo(prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (EOFException e) {
            Logger.e(LOG_TAG, "EOFException during network interception " + e.getMessage());

            return false; // Truncated UTF-8 sequence.
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean bodyHasUnsupportedEncoding(Headers headers) {
        String contentEncoding = headers.get("Content-Encoding");
        return contentEncoding != null &&
                !contentEncoding.equalsIgnoreCase("identity") &&
                !contentEncoding.equalsIgnoreCase("gzip");
    }

    private boolean bodyGzipped(Headers headers) {
        String contentEncoding = headers.get("Content-Encoding");
        return "gzip".equalsIgnoreCase(contentEncoding);
    }

    private String readFromBuffer(Buffer buffer, Charset charset) {
        long bufferSize = buffer.size();
        long maxBytes = Math.min(bufferSize, maxContentLength);
        String body = "";
        try {
            body = buffer.readString(maxBytes, charset);
        } catch (EOFException e) {
            Logger.e(LOG_TAG, "EOFException during network interception " + e.getMessage());

            body += Config.chuck_body_unexpected_eof;
        }
        if (bufferSize > maxContentLength) {
            body += Config.chuck_body_content_truncated;
        }

        return body;
    }

    private BufferedSource getNativeSource(BufferedSource input, boolean isGzipped) {
        if (isGzipped) {
            GzipSource source = new GzipSource(input);
            return Okio.buffer(source);
        } else {
            return input;
        }
    }

    private BufferedSource getNativeSource(Response response) throws IOException {
        if (response !=null && bodyGzipped(response.headers())) {
            BufferedSource source = response.peekBody(maxContentLength).source();

            if (source.buffer().size() < maxContentLength) {
                return getNativeSource(source, true);
            } else {
                Log.w(LOG_TAG, "gzip encoded response was too long");
            }
        }
        return response.body().source();
    }
}
