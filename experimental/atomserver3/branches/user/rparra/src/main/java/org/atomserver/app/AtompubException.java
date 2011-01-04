package org.atomserver.app;

import javax.ws.rs.core.Response;


public class AtompubException extends RuntimeException {
	private static final long serialVersionUID = -1348806532265362582L;
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
