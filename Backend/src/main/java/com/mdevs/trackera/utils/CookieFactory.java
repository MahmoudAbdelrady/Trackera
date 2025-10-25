package com.mdevs.trackera.utils;

import com.mdevs.trackera.config.general.AppConfig;
import jakarta.servlet.http.Cookie;
import org.springframework.stereotype.Component;

@Component
public class CookieFactory {
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

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
}
