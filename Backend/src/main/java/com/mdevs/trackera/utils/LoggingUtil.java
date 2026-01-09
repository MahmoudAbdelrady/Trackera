package com.mdevs.trackera.utils;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class LoggingUtil {
    public static Object proceedWithLogging(ProceedingJoinPoint joinPoint, Logger logger, String logInfo, List<Object> args) throws Throwable {
        logger.info("[START] " + logInfo, args.toArray());
        List<Object> extendedArgs = new ArrayList<>(args);
        try {
            long start = System.currentTimeMillis();
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            extendedArgs.add(duration);
            logger.info("[END] " + logInfo + " || took {} ms", extendedArgs.toArray());
            return result;
        } catch (Throwable throwable) {
            extendedArgs.add(throwable.getMessage());
            List<Object> errorArgs = new ArrayList<>(extendedArgs);
            errorArgs.add(throwable);
            logger.error("[ERROR] " + logInfo + " || Reason: {}", errorArgs.toArray());
            throw throwable;
        }
    }
}
