package com.mdevs.trackera.shared.exceptions.types;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public class JiraException extends RuntimeException {
    @Getter
    private final int statusCode;

    public JiraException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}
