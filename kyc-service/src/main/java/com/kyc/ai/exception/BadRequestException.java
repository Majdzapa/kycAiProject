package com.kyc.ai.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String exception) {
    super(exception);
    }
}
