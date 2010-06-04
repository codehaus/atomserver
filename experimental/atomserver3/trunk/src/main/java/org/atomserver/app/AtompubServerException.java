package org.atomserver.app;

import javax.ws.rs.core.Response;

public class AtompubServerException extends AtompubException {
    protected AtompubServerException(String message) {
        super(Response.Status.INTERNAL_SERVER_ERROR, message);
    }

    protected AtompubServerException(String message, Throwable cause) {
        super(Response.Status.INTERNAL_SERVER_ERROR, message, cause);
    }
}
