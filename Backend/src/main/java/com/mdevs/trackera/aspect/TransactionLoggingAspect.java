package com.mdevs.trackera.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Aspect
@Component
public class TransactionLoggingAspect {
    private final static Logger TRANSACTION_LOGGING_LOGGER = Logger.getLogger(TransactionLoggingAspect.class.getName());

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object logAroundTransactionMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String logInfo = "Transaction with method: " + joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
        return RequestLoggingAspect.getObject(joinPoint, logInfo, TRANSACTION_LOGGING_LOGGER);
    }
}
