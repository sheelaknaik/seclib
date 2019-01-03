package com.vodafone.lib.seclibng.auto;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;

import com.vodafone.lib.seclibng.Event;
import com.vodafone.lib.seclibng.SecLibNG;
import com.vodafone.lib.seclibng.comms.Config;
import com.vodafone.lib.seclibng.comms.EventConstants;
import com.vodafone.lib.seclibng.comms.Logger;

import org.json.JSONException;

public class FragmentLifeCycle implements LifecycleObserver {
    private final Lifecycle lifecycle;

    /***
     * enum for fragment lifecycle events
     */
    public enum LifecycleEvents {
        CREATED,
        STARTED,
        PAUSED,
        STOPPED,
        RESUMED,
        DESTROYED
    }

    private final String tagLifecycleLogger = "FragmentLifeCycle.";
    private android.support.v4.app.Fragment eventSupportFragment;

    public FragmentLifeCycle(android.support.v4.app.Fragment fragment,Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
        eventSupportFragment = fragment;
        lifecycle.addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void FragmentOnCreate() {
        try {
                SecLibNG.getInstance().logEvent(createFragmentEvent(eventSupportFragment, LifecycleEvents.CREATED.name()));
           } catch (JSONException e) {
            Logger.e(tagLifecycleLogger, "Error while creating event for CREATED fragment");
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void FragmentOnStart() {
        try {
             SecLibNG.getInstance().logEvent(createFragmentEvent(eventSupportFragment, LifecycleEvents.STARTED.name()));
        } catch (JSONException e) {
            Logger.e(tagLifecycleLogger, "Error while creating event for STARTED fragment");
        }
    }

    /***
     * Creates event for Fragment life cycle.
     *
     * @param fragment fragment name
     * @return returns the event object
     * @throws JSONException exception
     */
    private Event createFragmentEvent(android.support.v4.app.Fragment fragment, String lifecycleName) throws JSONException {
        try {
            Context context= fragment.getContext();
            Event event = new Event(Event.EventType.PAGE,context );
            event.addPayload(EventConstants.EVENT_DESCRIPTION, lifecycleName);
            event.addPayload(EventConstants.EVENT_ELEMENT, Config.DEFAULT_NA);
            event.addPayload(EventConstants.X_VF_PAGE, fragment.getClass().getName());
            event.addPayload(EventConstants.X_VF_SUB_PAGE, Config.DEFAULT_NA);
            return event;
        } catch (Exception e) {
            Logger.e(tagLifecycleLogger, "Unable to create Fragment event "+e.getMessage(), e);
        }
        return null;
    }



    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void FragmentOnResume() {
        try {
                SecLibNG.getInstance().logEvent(createFragmentEvent(eventSupportFragment, LifecycleEvents.RESUMED.name()));
        } catch (JSONException e) {
            Logger.e(tagLifecycleLogger, "Error while creating event for RESUMED fragment");
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void FragmentOnStop() {
        try {
                SecLibNG.getInstance().logEvent(createFragmentEvent(eventSupportFragment, LifecycleEvents.STOPPED.name()));
          } catch (JSONException e) {
            Logger.e(tagLifecycleLogger, "Error while creating event for STOPPED fragment");
        }
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void FragmentOnPause() {
        try {
                SecLibNG.getInstance().logEvent(createFragmentEvent(eventSupportFragment, LifecycleEvents.PAUSED.name()));
           } catch (JSONException e) {
            Logger.e(tagLifecycleLogger, "Error while creating event for PAUSED fragment");
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void FragmentOnDestroy() {
        try {
                SecLibNG.getInstance().logEvent(createFragmentEvent(eventSupportFragment, LifecycleEvents.DESTROYED.name()));
                lifecycle.removeObserver(this);

        } catch (JSONException e) {
            Logger.e(tagLifecycleLogger, "Error while creating event for DESTROYED fragment");
        }
    }
}
