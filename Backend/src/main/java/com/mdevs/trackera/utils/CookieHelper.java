package com.mdevs.trackera.utils;

import com.mdevs.trackera.config.general.AppConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class CookieHelper {
    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    public static final String COOKIE_GENERAL_PATH = "/trackera";

    public static final String COOKIE_AUTH_PATH = "/trackera/auth";

    public static final int REFRESH_TOKEN_ROTATION_THRESHOLD_DAYS = 3;

    public Cookie create(String name, String value, boolean isHttpOnly, String path, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(isHttpOnly);
        cookie.setSecure(AppConfig.isProductionEnv());
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);
        return cookie;
    }

    public static int getTokenCookieMaxAge(boolean isAccessToken) {
        return AppConfig.getApplicationContext().getEnvironment().getProperty(isAccessToken ? "trackera.cookie.access-token-max-age" : "trackera.cookie.refresh-token-max-age", Integer.class, 0);
    }

    public static String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue).findFirst().orElse(null);
    }
}
