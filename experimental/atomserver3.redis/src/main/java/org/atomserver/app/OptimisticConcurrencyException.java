package org.atomserver.app;

import javax.ws.rs.core.Response;

public class OptimisticConcurrencyException extends AtompubException {
    public OptimisticConcurrencyException(String message) {
        super(Response.Status.CONFLICT, message);
    }

    protected OptimisticConcurrencyException(String message, Throwable cause) {
        super(Response.Status.CONFLICT, message, cause);
    }
}