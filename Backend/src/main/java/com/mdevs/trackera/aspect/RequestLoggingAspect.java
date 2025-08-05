package com.mdevs.trackera.aspect;

import com.mdevs.trackera.config.AppConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.logging.Level;
import java.util.logging.Logger;

@Aspect
@Component
public class RequestLoggingAspect {
    private static final Logger REQUEST_LOGGING_LOGGER = Logger.getLogger(RequestLoggingAspect.class.getName());

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logAroundControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null)
            return joinPoint.proceed();

        HttpServletRequest request = attributes.getRequest();
        String requestCreator = AppConfig.getCurrentUser() != null ? " || User: " + AppConfig.getCurrentUser().getUsername() : "";

        String logInfo = "(" + request.getMethod() + ") Request URI: " + request.getRequestURI() + requestCreator;

        return getObject(joinPoint, logInfo, REQUEST_LOGGING_LOGGER);
    }

    public static Object getObject(ProceedingJoinPoint joinPoint, String logInfo, Logger logger) throws Throwable {
        logger.info("[START] " + logInfo);
        try {
            long start = System.currentTimeMillis();
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            logger.info("[END] " + logInfo + " || took " + duration + "ms");
            return result;
        } catch (Throwable throwable) {
            logger.log(Level.SEVERE, "[ERROR] " + logInfo + " || Reason: " + throwable.getMessage(), throwable);
            throw throwable;
        }
    }
}
