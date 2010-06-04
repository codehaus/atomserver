package org.atomserver.app;

public class DuplicateException extends AtompubException {
    public DuplicateException(String message) {
        super(Type.DUPLICATE, message);
    }
}