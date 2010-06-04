package org.atomserver.app;

public class NotFoundException extends AtompubException {
    public NotFoundException(String message) {
        super(Type.NOT_FOUND, message);
    }
}
