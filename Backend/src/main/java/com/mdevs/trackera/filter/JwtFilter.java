package com.mdevs.trackera.filter;

import com.mdevs.trackera.config.security.ApiConfig;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.service.UserService;
import com.mdevs.trackera.shared.annotations.PublicAPI;
import com.mdevs.trackera.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    private final ApiConfig apiConfig;

    private final UserService userService;

    public JwtFilter(JwtUtil jwtUtil, ApiConfig apiConfig, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.apiConfig = apiConfig;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String jwt = jwtUtil.getToken(request);
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
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return apiConfig.hasAnnotations(request.getRequestURI(), request.getMethod(), Set.of(PublicAPI.class));
    }
}
