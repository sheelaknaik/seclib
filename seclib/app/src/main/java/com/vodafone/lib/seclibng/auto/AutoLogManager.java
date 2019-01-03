package com.vodafone.lib.seclibng.auto;

import android.app.Application;

/**
 * This Class will enable the life cycle events .
 */

public class AutoLogManager {

    private AutoLifecycleLogger mLifecycleLogger;

    /***
     * Constructor for activity lifecycle events
     *
     * @param app application object
     */
    public AutoLogManager(Application app) {

        mLifecycleLogger = new AutoLifecycleLogger(app);
    }

    /***
     * start activity lifecycle callbacks
     */
    public void registerActivityLifecycleCallbacks() {
        mLifecycleLogger.registerActivityLifecycleCallbacks();
    }

    /****
     * Stop Activity lifecycle call backs
     */
    public void unregisterActivityLifecycleCallbacks() {
        mLifecycleLogger.unregisterActivityLifecycleCallbacks();
    }


}
