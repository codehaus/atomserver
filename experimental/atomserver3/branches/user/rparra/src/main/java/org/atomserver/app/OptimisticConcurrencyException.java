package org.atomserver.app;

import javax.ws.rs.core.Response;

public class OptimisticConcurrencyException extends AtompubException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 7090397392216524068L;

	public OptimisticConcurrencyException(String message) {
        super(Response.Status.CONFLICT, message);
    }

    protected OptimisticConcurrencyException(String message, Throwable cause) {
        super(Response.Status.CONFLICT, message, cause);
    }
}