package com.vodafone.lib.seclibng.comms;

import java.io.IOException;

/***
 * Custom Exception class for handling exception
 */
public class ProtocolException extends IOException {

    public ProtocolException() {
        super();
    }

    public ProtocolException(String message) {
        super(message);

    }
}