package com.mdevs.trackera.aspect;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.shared.utils.LoggingUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Aspect
@Component
public class RequestLoggingAspect {

    private static final Logger REQUEST_LOGGER = LoggerFactory.getLogger(RequestLoggingAspect.class);

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logAroundControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        User requestUser = AppConfig.getCurrentUser();

        String userInfo = (requestUser != null) ? String.format("[id=%s, name=%s]", requestUser.getId(), requestUser.getFirstname()) : "[Anonymous]";

        String logInfo = "({}) Request URI: {} || User: {}";

        List<Object> logArgs = List.of(request.getMethod(), request.getRequestURI(), userInfo);

        return LoggingUtil.proceedWithLogging(joinPoint, REQUEST_LOGGER, logInfo, logArgs);
    }
}