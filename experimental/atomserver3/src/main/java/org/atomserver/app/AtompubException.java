package org.atomserver.app;

import org.atomserver.ext.Status;
import org.atomserver.app.jaxrs.AbderaMarshaller;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class AtompubException extends WebApplicationException {
    public AtompubException(int code, String message) {
        super(createResponse(code, message));
    }

    private static Response createResponse(int code, String message) {
        Status status = new Status(AbderaMarshaller.factory());
        status.setStatusCode(code);
        status.setMessage(message);
        return Response.status(code).entity(status).build();
    }
}
