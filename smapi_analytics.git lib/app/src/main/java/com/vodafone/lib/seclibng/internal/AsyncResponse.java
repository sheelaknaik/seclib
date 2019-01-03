package com.vodafone.lib.seclibng.internal;

/**
 * to identify if events are pushed
 */

public interface AsyncResponse {
    @SuppressWarnings("SameParameterValue")
    void processFinish(boolean output);
}