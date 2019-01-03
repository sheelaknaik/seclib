package com.vodafone.lib.seclibng;

import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.vodafone.lib.seclibng.Event;
import com.vodafone.lib.seclibng.SecLibNG;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

@Aspect
public class WebViewAspect {

    private static final String LOG_TAG = "WebViewAspect";


    @Pointcut("(call(* android.webkit.WebView.loadUrl(..)) && !within(com.vodafone.lib.*..*))")
    public void loadWebView(JoinPoint joinPoint) {
        // afterLoadWebView()
    }

    @Before("loadWebView(*)")
    public void afterLoadWebView(final JoinPoint joinPoint) throws Exception {
        System.out.println(LOG_TAG + "====loadWebView====");
        Object[] args = joinPoint.getArgs();
        JSONObject responseObj = new JSONObject();
        responseObj.put("URL", args[0].toString());
        SecLibNG.getInstance().logNetworkevent("WebView",responseObj.toString(), Event.EventType.NETWORK,"");
        ;
    }
}
