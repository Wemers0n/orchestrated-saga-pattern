package com.ms.orchestrated.productvalidationservice.config.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionGlobalHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<?> handlerValidationException(ValidationException exception){
        var details = new ExceptionDetails(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
        return new ResponseEntity<>(details, HttpStatus.BAD_REQUEST);
    }
}
