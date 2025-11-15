package com.mdevs.trackera.utils;

import com.mdevs.trackera.config.general.AppConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class CookieHelper {
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    public static final int REFRESH_TOKEN_ROTATION_THRESHOLD_DAYS = 3;

    public Cookie create(String name, String value, boolean isHttpOnly, String path, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(isHttpOnly);
        cookie.setSecure(AppConfig.isProductionEnv());
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);
        return cookie;
    }

    public static int getRefreshTokenCookieMaxAge() {
        return AppConfig.getApplicationContext().getEnvironment().getProperty("trackera.cookie.max-age", Integer.class, 3600);
    }

    public String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue).findFirst().orElse(null);
    }
}
