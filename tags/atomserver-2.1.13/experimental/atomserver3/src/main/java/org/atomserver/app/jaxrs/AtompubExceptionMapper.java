package org.atomserver.app.jaxrs;

import org.atomserver.app.AtompubException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AtompubExceptionMapper implements ExceptionMapper<AtompubException> {

    public Response toResponse(AtompubException exception) {
        Response.Status status;
        switch (exception.type) {
        case BAD_REQUEST:
            status = Response.Status.BAD_REQUEST;
            break;
        case DUPLICATE:
            status = Response.Status.CONFLICT;
            break;
        case NOT_FOUND:
            status = Response.Status.NOT_FOUND;
            break;
        case OPTIMISTIC_CONCURRENCY:
            status = Response.Status.CONFLICT;
            break;
        default:
            return Response.serverError().entity(
                    String.format("Unknown Exception Type %s", exception.type)).build();
        }
        return Response.status(status)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .entity(exception.getMessage())
                .build();
    }
}