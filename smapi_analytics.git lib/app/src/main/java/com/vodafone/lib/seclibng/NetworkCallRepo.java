package com.vodafone.lib.seclibng;

import com.vodafone.lib.seclibng.comms.EventConstants;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkCallRepo {
    private static NetworkCallRepo sharedInstance;
    private ConcurrentHashMap<Long, URL> urls;
    private ConcurrentHashMap<Long, Event> networkEvent;
    private ConcurrentHashMap<Long, Long> requestStartTime;


    public NetworkCallRepo() {
        urls = new ConcurrentHashMap<>();
        networkEvent = new ConcurrentHashMap<>();
        requestStartTime = new ConcurrentHashMap<>();
    }

    static NetworkCallRepo getSharedInstance() {
        if (sharedInstance == null) {
            sharedInstance = new NetworkCallRepo();
        }
        return sharedInstance;
    }

    void setUrl(Long identity, URL url) {
        if (!this.urls.containsKey(identity) && SecLibNG.getInstance().getAppContext()!=null) {
            this.urls.put(identity, url);
            Event event = new Event(Event.EventType.NETWORK, SecLibNG.getInstance().getAppContext());
            event.addPayload(EventConstants.EVENT_DESCRIPTION, "{}");

            this.networkEvent.put(identity, event);
            this.requestStartTime.put(identity, System.currentTimeMillis());
        }
    }

    void removeURL(Long identity) {
        this.urls.remove(identity);
    }

    void setNetworkEvent(Long identity, Event event) {
        if (this.urls.containsKey(identity)) {
            this.networkEvent.put(identity, event);
        }
    }

    Event getNetworkEvent(Long identity) {
        if (this.urls.containsKey(identity)) {
            return this.networkEvent.get(identity);
        }
        return null;
    }

    Long getRequestStartTime(Long identity) {
        if (this.urls.containsKey(identity)) {
            return this.requestStartTime.get(identity);
        }
        return null;
    }

    void setRequestStartTime(Long identity, Long startTime) {
        if (this.urls.containsKey(identity)) {
            this.requestStartTime.put(identity, startTime);
        }
    }
}