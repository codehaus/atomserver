package org.atomserver.app;

import javax.ws.rs.core.Response;


public class AtompubException extends RuntimeException {
    public final Response.Status status;

    protected AtompubException(Response.Status status, String message) {
        super(message);
        this.status = status;
    }

    protected AtompubException(Response.Status status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
