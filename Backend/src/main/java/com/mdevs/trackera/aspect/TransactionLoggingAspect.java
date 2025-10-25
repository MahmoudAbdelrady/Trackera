package com.mdevs.trackera.aspect;

import com.mdevs.trackera.utils.LoggingUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Aspect
@Component
public class TransactionLoggingAspect {
    private final static Logger TRANSACTION_LOGGER = LoggerFactory.getLogger(TransactionLoggingAspect.class);

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object logAroundTransactionMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String declaringClass = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        String logInfo = "Transaction with method: {}.{}";
        List<Object> logArgs = List.of(declaringClass, methodName);

        return LoggingUtil.proceedWithLogging(joinPoint, TRANSACTION_LOGGER, logInfo, logArgs);
    }
}
