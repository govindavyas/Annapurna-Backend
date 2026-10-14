package com.localfood.backend.common;

public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }
}
