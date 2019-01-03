package com.vodafone.lib.seclibng.network;

import com.vodafone.lib.seclibng.Event;
import com.vodafone.lib.seclibng.SecLibNG;
import com.vodafone.lib.seclibng.comms.EventConstants;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.HttpUrl;

public class OkHttpCallRepo {
    private static OkHttpCallRepo sharedInstance;
    private ConcurrentHashMap<Long, HttpUrl> urls;
    private ConcurrentHashMap<Long, Event> networkEvent;
    private ConcurrentHashMap<Long, Long> requestStartTime;


    public OkHttpCallRepo() {
        urls = new ConcurrentHashMap<>();
        networkEvent = new ConcurrentHashMap<>();
        requestStartTime = new ConcurrentHashMap<>();
    }

    static OkHttpCallRepo getSharedInstance() {
        if (sharedInstance == null) {
            sharedInstance = new OkHttpCallRepo();
        }
        return sharedInstance;
    }

    void setUrl(Long identity, HttpUrl url) {
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