package org.atomserver.app;

import javax.ws.rs.core.Response;

public class BadRequestException extends AtompubException {
    public BadRequestException(String message) {
        super(Response.Status.BAD_REQUEST, message);
    }

    protected BadRequestException(String message, Throwable cause) {
        super(Response.Status.BAD_REQUEST, message, cause);
    }
}
