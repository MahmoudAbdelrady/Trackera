package com.mdevs.trackera.filter;

import com.mdevs.trackera.shared.annotations.PublicAPI;
import io.micrometer.common.lang.NonNull;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Slf4j
public abstract class TrackeraSecurityFilter extends OncePerRequestFilter {
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        if (request.getRequestURI().startsWith("/actuator") || request.getServletPath().startsWith("/app-docs")) {
            return true;
        }
        try {
            HandlerExecutionChain chain = handlerMapping.getHandler(request);
            if (chain != null && chain.getHandler() instanceof HandlerMethod handlerMethod) {
                return handlerMethod.hasMethodAnnotation(PublicAPI.class) || handlerMethod.getBeanType().isAnnotationPresent(PublicAPI.class);
            }
        } catch (Exception e) {
            log.error("Could not resolve handler for request: {}", request.getRequestURI(), e);
        }
        return false;
    }
}
