package com.mdevs.trackera.utils;

import com.mdevs.trackera.shared.exceptions.TrackeraExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ExceptionResponseMaker {
    private static final TrackeraExceptionResponse trackeraExceptionResponse = new TrackeraExceptionResponse();

    public static ResponseEntity<?> makeResponse(String message, HttpStatus status) {
        return makeResponse(message, null, status);
    }

    public static ResponseEntity<?> makeResponse(String message, Object data, HttpStatus status) {
        trackeraExceptionResponse.setMessage(message);
        trackeraExceptionResponse.setData(data);
        return new ResponseEntity<>(trackeraExceptionResponse, status);
    }
}
