package org.atomserver.app;

import javax.ws.rs.core.Response;

public class NotFoundException extends AtompubException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 3605198470996464259L;

	public NotFoundException(String message) {
        super(Response.Status.NOT_FOUND, message);
    }

    protected NotFoundException(String message, Throwable cause) {
        super(Response.Status.NOT_FOUND, message, cause);
    }
}
