package com.vodafone.lib.seclibng.internal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.vodafone.lib.seclibng.Event;
import com.vodafone.lib.seclibng.comms.Config;
import com.vodafone.lib.seclibng.comms.Logger;
import com.vodafone.lib.seclibng.comms.Protocol;
import com.vodafone.lib.seclibng.comms.ProtocolException;
import com.vodafone.lib.seclibng.comms.SharedPref;

/**
 * Send events to the server
 */
public class AsyncPushEvents extends AsyncTask<Event, Void, String> {

    @SuppressLint("StaticFieldLeak")
    private Context mContext;
    private Protocol mProtocol;
    public AsyncResponse delegate=null;

    private static final String eventAsyncTag = "AsyncPushEvents";

    public AsyncPushEvents (Context context, Protocol protocol){
        mContext = context;
        mProtocol = protocol;
    }

    @Override
    protected String doInBackground(Event... events) {
        try {
            mProtocol.sendEvents(events);
            return Config.DEFAULT_RESULT_SUCCESS;
        } catch (ProtocolException spe) {
            Logger.e(eventAsyncTag, "Error "+spe.getMessage(), spe);
        }
        return Config.DEFAULT_RESULT_FAIL;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onPostExecute(String aVoid) {
        super.onPostExecute(aVoid);

        SharedPref.setConfigKeys(mContext,Config.KEYNAME_SEND_EVENT_SERVICE_RESULT_STATUS,aVoid);

        if(delegate !=null)
            delegate.processFinish(true);
    }
}


