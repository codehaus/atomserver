package org.atomserver.app;

public class OptimisticConcurrencyException extends AtompubException {
    public OptimisticConcurrencyException(String message) {
        super(Type.OPTIMISTIC_CONCURRENCY, message);
    }
}