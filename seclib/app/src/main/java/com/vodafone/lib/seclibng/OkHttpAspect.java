package com.vodafone.lib.seclibng;

import com.vodafone.lib.seclibng.SecLibNG;
import com.vodafone.lib.seclibng.network.OkHTTPInterceptor;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;


@Aspect
public class OkHttpAspect {

    private static final String LOG_TAG = "OkHttpAspect";

    @Pointcut("(call(* com.squareup.okhttp.OkHttpClient.open(java.net.URL)) && (args(url) && !within(com.vodafone.lib.*..*)))")
    public void okHttpClientOpen(JoinPoint joinPoint) {
        // afterOkHttpClientOpen()
    }

    @Before("okHttpClientOpen(*)")
    public void afterOkHttpClientOpen(final JoinPoint joinPoint) throws Exception {
        System.out.println(LOG_TAG + "====" + "okHttpClientOpen");

    }


    @Pointcut("(call(* com.squareup.okhttp.OkHttpClient.newCall(..)) && !within(com.vodafone.lib.*..*))")
    public void okHttpBuilderbuild(JoinPoint joinPoint) {
        // afterOkHttpBuilderbuild()
    }

    @Before("okHttpBuilderbuild(*)")
    public void afterOkHttpBuilderbuild(final JoinPoint joinPoint) throws Exception {
        System.out.println(LOG_TAG + "====okHttpBuilderbuild");
        if(joinPoint.getTarget() instanceof  com.squareup.okhttp.OkHttpClient) {
            com.squareup.okhttp.OkHttpClient client = (com.squareup.okhttp.OkHttpClient) joinPoint.getTarget();
            client.interceptors().add(new OkHTTPInterceptor(SecLibNG.getInstance().getAppContext()));
        }
    }
}
