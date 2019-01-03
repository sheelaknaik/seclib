/*
 * Copyright (C) 2013 Vodafone Group Services GmbH.  All Rights Reserved.
 */

package com.vodafone.lib.seclibng.comms;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Arrays;

import android.content.Context;

import com.vodafone.lib.seclibng.Event;
import com.vodafone.lib.seclibng.SecLibNG;
import com.vodafone.lib.seclibng.storage.SqliteDb;

/**
 * The CrashHandler class handles uncaught exceptions from the code by sending
 * the exception to the SecLib so that it will be stored persistently until it's
 * sent to the back end with the corresponding stack trace.
 */
public final class CrashHandler implements UncaughtExceptionHandler {
    private SqliteDb db;

    /**
     * Singleton instance.
     **/
    private static CrashHandler sInstance = null;
    private static final String TAG_CRASH_HANDLER = "CrashHandler";

    /**
     * Default handler set by the platform.
     **/
    private UncaughtExceptionHandler mDefaultHandler = null;

    /**
     * Android context.
     **/
    private Context mContext;

    /**
     * The constructor.
     *
     * @param context Android context.
     */
    private CrashHandler(final Context context) {
        mContext = context;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    /**
     * Initialize the default crash handler.
     *
     * @param context Android Context (must be Application Context to avoid
     *                memory leaks).
     */
    public static synchronized void init(final Context context) {
        if (sInstance == null) {
            sInstance = new CrashHandler(context);
            Thread.setDefaultUncaughtExceptionHandler(sInstance);
        }
    }

    @Override
    public void uncaughtException(final Thread thread,
                                  final Throwable throwable) {

        /** Update event type from Exception to Crash **/
        try {
            String transactionId = SharedPref.getConfigKeys(mContext, Config.KEYNAME_EXCEPTION_TRANSACTION_ID, "NA");

            if (Event.getPendingEvents() != null) {

                for (Event event : Event.getPendingEvents()) {
                    //Update
                    if (event.getTransactionId() == transactionId) {
                        event.setEventType("Crash");
                    }
                }
            } else {
                SecLibNG.getInstance().logEventException(throwable.getClass().getCanonicalName() == null ? Config.DEFAULT_NA : throwable.getClass().getCanonicalName(), throwable.getMessage() == null ? Config.DEFAULT_NA : throwable.getMessage(), "NA", Arrays.toString(throwable.getStackTrace()), true);
            }
        } catch (IllegalStateException e) {
            Logger.e(TAG_CRASH_HANDLER, "Ignoring IllegalStateException: " + e.getMessage());
        } catch (Exception spe) {
            Logger.e(TAG_CRASH_HANDLER, "Ignoring Exception: " + spe.getMessage());
        }

        /** Pass the throwable back to the native throwable handler. **/
        if (mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, throwable);
        }
    }
}
