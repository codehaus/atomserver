package org.atomserver.app;

public class BadRequestException extends AtompubException {
    public BadRequestException(String message) {
        super(Type.BAD_REQUEST, message);
    }
}
