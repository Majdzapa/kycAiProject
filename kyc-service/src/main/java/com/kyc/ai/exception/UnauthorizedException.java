package com.kyc.ai.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String exception) {
    super(exception);
    }
}
