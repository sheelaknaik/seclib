package com.vodafone.lib.seclibng;

import com.vodafone.lib.seclibng.SecLibNG;
import com.vodafone.lib.seclibng.comms.Logger;
import com.vodafone.lib.seclibng.network.NetworkInterceptor;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import okhttp3.OkHttpClient;


public class OkHttp3Aspect {
    private static final String LOG_TAG = "OkHttp3Aspect";

    @Aspect
    public class NetworkAspect {
        @Pointcut("(call(* okhttp3.OkHttpClient.Builder.build(..)) && !within(com.vodafone.lib..*))")
        public void okHttp3RequestBuilderbuild(JoinPoint joinPoint) {
            // afterOkHttp3RequestBuilderbuild()
        }


        @Before("okHttp3RequestBuilderbuild(*)")
        public void afterOkHttp3RequestBuilderbuild(final JoinPoint joinPoint) throws Exception {
            //Logger.i(LOG_TAG, "okHttp3RequestBuilderbuild");
            System.out.println(LOG_TAG + "====" + "okHttp3RequestBuilderbuild");
            if (joinPoint.getTarget() instanceof OkHttpClient.Builder) {
                try {
                    OkHttpClient.Builder client = (OkHttpClient.Builder) joinPoint.getTarget();
                    // okhttp3.Request request = (okhttp3.Request) builder.build();

                    client.addNetworkInterceptor(new NetworkInterceptor(SecLibNG.getInstance().getAppContext()));


                } catch (Exception e) {
                    Logger.i(com.vodafone.lib.seclibng.NetworkAspect.class.getName(), e.getMessage());
                }
            }
        }


        @Pointcut("(call(okhttp3.Request okhttp3.Request.Builder.build(..)) && !within(com.vodafone.lib..*))")
        public void okHttp3Builderbuild(JoinPoint joinPoint) {
            // afterOkHttp3Builderbuild()
        }

        @Before("okHttp3Builderbuild(*)")
        public void afterOkHttp3Builderbuild(final JoinPoint joinPoint) throws Exception {
            System.out.println(LOG_TAG + "====" + "okHttp3Builderbuild");
        }



        @Pointcut("call(* okhttp3..*(..))")
        public void callOkHttp3(JoinPoint joinPoint) {
            // aftercallOkHttp3()
        }

        @Before("callOkHttp3(*)")
        public void aftercallOkHttp3(final JoinPoint joinPoint) throws Exception {
            System.out.println(LOG_TAG + "====" + "callOkHttp3");
        }



    }
}
