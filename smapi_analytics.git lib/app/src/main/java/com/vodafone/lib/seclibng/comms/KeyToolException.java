package com.vodafone.lib.seclibng.comms;

import java.io.IOException;

/***
 * Protocol Exception types
 */
public class KeyToolException extends IOException {

    /***
     * Default Constructor for KEYTOOL Exception
     *
     * @param exc Exception message
     */
    public KeyToolException(String exc) {
        super(exc);
    }

    /***
     * Default Constructor for KEYTOOL Exception
     *
     * @param exc  Exception message
     * @param thr, Throwable
     */
    public KeyToolException(String exc, Throwable thr) {
        super(exc, thr);
        Logger.e(exc, thr.getMessage(), thr);

    }
}