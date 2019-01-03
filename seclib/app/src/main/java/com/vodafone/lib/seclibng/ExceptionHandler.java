package com.vodafone.lib.seclibng;

import com.vodafone.lib.seclibng.comms.Config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.Arrays;

/**
 * Exception handling class
 */
@Aspect
public class ExceptionHandler {
    /***
     * Logs the exception details
     *
     */
    private static final String TAG ="ExceptionHandler";

    @Pointcut("execution(* *(..))")
    public void exceptionEntryPoint() {
    }

    @AfterThrowing(pointcut = "exceptionEntryPoint()", throwing = "throwable")
    public void ExceptionLogging(JoinPoint joinPoint, Throwable throwable) {
        SecLibNG.getInstance().logEventException(throwable.getClass().getCanonicalName()==null? Config.DEFAULT_NA:throwable.getClass().getCanonicalName(),throwable.getMessage()==null?Config.DEFAULT_NA:throwable.getMessage(),Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()), Arrays.toString(throwable.getStackTrace()), false);

    }
}
