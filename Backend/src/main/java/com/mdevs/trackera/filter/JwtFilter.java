package com.mdevs.trackera.filter;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.service.UserService;
import com.mdevs.trackera.shared.annotations.PublicAPI;
import com.mdevs.trackera.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    private final UserService userService;

    private final RequestMappingHandlerMapping handlerMapping;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String jwt = jwtUtil.getToken(request, true);
        if (jwt == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Claims claims;
        try {
            claims = jwtUtil.validateAndGetTokenPayload(jwt, true);
        } catch (SecurityException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        User user = userService.findByUuidOrThrow(claims.get("id", String.class));
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
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
