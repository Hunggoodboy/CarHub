package com.exception;

// exception/NotPurchasedException.java
public class NotPurchasedException extends RuntimeException {
    public NotPurchasedException(String message) {
        super(message);
    }
}