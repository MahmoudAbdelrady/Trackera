package com.mdevs.trackera.aspect;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.shared.rate_limit.ApiRateLimiter;
import com.mdevs.trackera.shared.annotations.RateLimited;
import com.mdevs.trackera.shared.exceptions.types.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    private final ApiRateLimiter apiRateLimiter;

    private final HttpServletRequest request;

    @Around("@within(rateLimited) || @annotation(rateLimited)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        if (rateLimited == null) {
            rateLimited = joinPoint.getTarget().getClass().getAnnotation(RateLimited.class); // Get class-level annotation if method-level is not present
        }

        if (apiRateLimiter.tryConsume(resolveClientKey(), request.getRequestURI(), rateLimited.permitsPerMinute(), rateLimited.refillIntervalMinutes())) {
            return joinPoint.proceed();
        } else {
            throw new RateLimitExceededException("Too many requests - Rate limit exceeded");
        }
    }

    private String resolveClientKey() {
        User currentUser = AppConfig.getCurrentUser();
        if (currentUser != null) {
            return String.join("_", currentUser.getId().toString(), currentUser.getFirstname());
        }
        return request.getRemoteAddr();
    }
}
