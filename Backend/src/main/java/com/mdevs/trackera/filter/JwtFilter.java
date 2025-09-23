package com.mdevs.trackera.filter;

import com.mdevs.trackera.config.security.ApiConfig;
import com.mdevs.trackera.dto.auth.AuthFilterUserDTO;
import com.mdevs.trackera.shared.annotations.PublicAPI;
import com.mdevs.trackera.shared.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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

    @Autowired
    public JwtFilter(JwtUtil jwtUtil, ApiConfig apiConfig) {
        this.jwtUtil = jwtUtil;
        this.apiConfig = apiConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String jwtTokenHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isEmpty(jwtTokenHeader) || !jwtTokenHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String jwtToken = jwtTokenHeader.substring(7);
        Claims claims;
        try {
            claims = jwtUtil.validateAndGetTokenPayload(jwtToken, true);
        } catch (SecurityException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        AuthFilterUserDTO authFilterUserDTO = new AuthFilterUserDTO(claims.get("email", String.class));
        Authentication authentication = new UsernamePasswordAuthenticationToken(authFilterUserDTO, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return apiConfig.hasAnnotations(request.getRequestURI(), request.getMethod(), Set.of(PublicAPI.class));
    }
}
