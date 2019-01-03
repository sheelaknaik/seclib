package com.vodafone.lib.seclibng.comms;

import android.util.Log;

import com.vodafone.lib.seclibng.SecLibNG;

/**
 * Alternative For Log class which contains al Log methods and controls whether print those in logcat or not.
 */

public class Logger {
    private static final  String SECLIB="SecLib:";

    /***
     * Private Constructor
     */
    private Logger() {
    }

    /***
     * Alternative for Log.i
     *
     * @param tag     Tag for the error message
     * @param message Message to display
     */
    public static void i(String tag, String message) {
        if (SecLibNG.getInstance().getVerboseStatus())
            Log.i(SECLIB+tag, message);
    }

    /***
     * Alternative for Log.w with Throwable
     *
     * @param tag     Tag for the error message
     * @param message Message to display
     * @param thr     Throwable
     */
    public static void w(String tag, String message, Throwable thr) {
        if (SecLibNG.getInstance().getVerboseStatus())
            Log.w(SECLIB+tag, message, thr);
    }

    /***
     * Alternative for Log.w
     *
     * @param tag     Tag for the error message
     * @param message Message to display
     */

    public static void w(@SuppressWarnings("SameParameterValue") String tag, @SuppressWarnings("SameParameterValue") String message) {
        if (SecLibNG.getInstance().getVerboseStatus())
            Log.w(SECLIB+tag, message);
    }

    /***
     * Alternative for Log.d with Throwable
     *
     * @param tag     Tag for the error message
     * @param message Message to display
     * @param thr     Throwable
     */

    public static void d(@SuppressWarnings("SameParameterValue") String tag, String message, Throwable thr) {
        if (SecLibNG.getInstance().getVerboseStatus())
            Log.d(SECLIB+tag, message, thr);
    }

    /***
     * Alternative for Log.d with Throwable
     *
     * @param tag     Tag for the error message
     * @param message Message to display
     */

    public static void d(String tag, String message) {
        if (SecLibNG.getInstance().getVerboseStatus())
            Log.d(SECLIB+tag, message);
    }

    /***
     * Alternative for Log.e with Throwable
     *
     * @param tag     Tag for the error message
     * @param message Message to display
     * @param thr     Throwable
     */

    public static void e(String tag, String message, Throwable thr) {
        if (SecLibNG.getInstance().getVerboseStatus())
            Log.e(SECLIB+tag, message, thr);
    }

    /***
     * Alternative for Log.e
     *
     * @param tag     Tag for the error message
     * @param message Message to display
     */
    public static void e(String tag, String message) {
        if (SecLibNG.getInstance().getVerboseStatus())
            Log.e(SECLIB+tag, message);
    }

    /***
     * Alternative for Log.v
     *
     * @param tag     Tag for the error message
     * @param message Message to display
     */
    public static void v(String tag, String message) {
        if (SecLibNG.getInstance().getVerboseStatus())
            Log.v(SECLIB+tag, message);
    }
}
