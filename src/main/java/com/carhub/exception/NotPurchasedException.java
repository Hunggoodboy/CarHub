package com.carhub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class NotPurchasedException extends RuntimeException {

    public NotPurchasedException(String message) {
        super(message);
    }

}