package com.vodafone.lib.seclibng;

import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.vodafone.lib.seclibng.SecLibNG;
import com.vodafone.lib.seclibng.network.NetworkInterceptor;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

@Aspect
public class RetrofitAspect {

    private static final String LOG_TAG = "RetrofitAspect";


    @Pointcut("(call(* retrofit2.Retrofit.Builder.build(..)) && !within(com.vodafone.lib.*..*))")
    public void retrofitRequestBuilderbuild(JoinPoint joinPoint) {
        // afterOkHttp3RequestBuilderbuild()
    }

    @Before("retrofitRequestBuilderbuild(*)")
    public void afterOkHttp3RequestBuilderbuild(final JoinPoint joinPoint) throws Exception {
        System.out.println(LOG_TAG + "====retrofitRequestBuilderbuild");
        if(joinPoint.getTarget() instanceof Retrofit.Builder){
            retrofit2.Retrofit.Builder builder = (retrofit2.Retrofit.Builder)joinPoint.getTarget();
            OkHttpClient client = new OkHttpClient.Builder()
                    .addNetworkInterceptor(new NetworkInterceptor(SecLibNG.getInstance().getAppContext()))
                    .build();

            builder.client(client);
        }

    }
}
