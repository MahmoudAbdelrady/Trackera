package com.mdevs.trackera.filter;

import com.mdevs.trackera.utils.CookieHelper;
import com.mdevs.trackera.utils.CsrfUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CsrfTokenFilter extends TrackeraSecurityFilter {
    private final CsrfUtil csrfUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String csrfCookieToken = CookieHelper.extractCookieValue(request, CookieHelper.CSRF_COOKIE_NAME);
        String csrfHeaderToken = request.getHeader(CsrfUtil.CSRF_HEADER_NAME);

        try {
            csrfUtil.validate(csrfCookieToken, csrfHeaderToken);
        } catch (Exception e) {
            log.warn("CSRF token validation failed");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
