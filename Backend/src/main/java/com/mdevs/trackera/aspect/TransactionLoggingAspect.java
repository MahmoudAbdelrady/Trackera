package com.mdevs.trackera.aspect;

import com.mdevs.trackera.utils.LoggingUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.List;

@Aspect
@Component
@Slf4j
public class TransactionLoggingAspect {
    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object logAroundTransactionMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String declaringClass = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        String logInfo = "Transaction with method: {}.{}";
        List<Object> logArgs = List.of(declaringClass, methodName);

        return LoggingUtil.proceedWithLogging(joinPoint, log, logInfo, logArgs);
    }
}
