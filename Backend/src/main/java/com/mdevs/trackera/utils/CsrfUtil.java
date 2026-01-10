package com.mdevs.trackera.utils;

import com.mdevs.trackera.shared.exceptions.types.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CsrfUtil {
    public static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";

    public String generate() {
        return UUID.randomUUID().toString();
    }

    public void validate(String tokenFromCookie, String tokenFromHeader) {
        if (StringUtils.isEmpty(tokenFromCookie) || !tokenFromCookie.equals(tokenFromHeader)) {
            throw new UnauthorizedException("Invalid CSRF token.");
        }
    }
}
