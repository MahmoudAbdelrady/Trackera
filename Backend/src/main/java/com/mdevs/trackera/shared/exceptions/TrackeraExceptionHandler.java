package com.mdevs.trackera.shared.exceptions;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.shared.utils.response.ResponseMaker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class TrackeraExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException ex) {
        return new ResponseEntity<>(ResponseMaker.makeResponse(ex.getMessage(), null), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> handleNotFoundException(NotFoundException ex) {
        return new ResponseEntity<>(ResponseMaker.makeResponse(ex.getMessage(), null), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorizedException(UnauthorizedException ex) {
        return new ResponseEntity<>(ResponseMaker.makeResponse(ex.getMessage(), null), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> handleSecurityException(SecurityException exception) {
        return new ResponseEntity<>(ResponseMaker.makeResponse(exception.getMessage(), null), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException exception) {
        List<Map<String, String>> errorsList = exception.getBindingResult().getAllErrors().stream().map(error -> {
            Map<String, String> errorMap = new HashMap<>();
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errorMap.put("field", fieldName);
            errorMap.put("message", errorMessage);
            return errorMap;
        }).toList();
        return new ResponseEntity<>(ResponseMaker.makeResponse("Validation Error", errorsList), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleResourceNotFoundException(NoResourceFoundException exception) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Resource not found");
        response.put("message", "The requested resource was not found");
        response.put("path", exception.getResourcePath());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception exception) {
        String environment = AppConfig.getApplicationContext().getEnvironment().getProperty("trackera.environment");
        return new ResponseEntity<>(ResponseMaker.makeResponse(String.valueOf(environment).equals("dev") ? exception.getMessage() : "Something went wrong", null), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
