package org.atomserver.app;

public class AtompubException extends RuntimeException {
    public enum Type {
        BAD_REQUEST, DUPLICATE, NOT_FOUND, OPTIMISTIC_CONCURRENCY
    }

    public final Type type;

    protected AtompubException(Type type, String message) {
        super(message);
        this.type = type;
    }
}
