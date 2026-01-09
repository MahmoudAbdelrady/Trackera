package com.mdevs.trackera.shared.exceptions;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.shared.exceptions.types.*;
import com.mdevs.trackera.utils.ExceptionResponseMaker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class TrackeraExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException ex) {
        return ExceptionResponseMaker.makeResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> handleNotFoundException(NotFoundException ex) {
        return ExceptionResponseMaker.makeResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorizedException(UnauthorizedException ex) {
        return ExceptionResponseMaker.makeResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> handleSecurityException(SecurityException exception) {
        return ExceptionResponseMaker.makeResponse(exception.getMessage(), HttpStatus.UNAUTHORIZED);
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
        return ExceptionResponseMaker.makeResponse("Validation Error", errorsList, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleResourceNotFoundException(NoResourceFoundException exception) {
        return ExceptionResponseMaker.makeResponse("The requested resource [" + exception.getResourcePath() + "] was not found", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxSizeException(MaxUploadSizeExceededException exception) {
        return ExceptionResponseMaker.makeResponse("File size exceeds the allowed limit of 5MB", HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(JiraException.class)
    public ResponseEntity<?> handleJiraException(JiraException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ExceptionResponseMaker.makeResponse(exception.getMessage(), status);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<?> handleRateLimitExceededException(RateLimitExceededException exception) {
        return ExceptionResponseMaker.makeResponse(exception.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception exception) {
        return ExceptionResponseMaker.makeResponse(AppConfig.isProductionEnv() ? "Something went wrong" : exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
