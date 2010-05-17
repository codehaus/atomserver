package org.atomserver.app.jaxrs;

import org.atomserver.app.AtompubException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AtompubExceptionMapper implements ExceptionMapper<AtompubException> {

    public Response toResponse(AtompubException exception) {
        return Response.status(exception.status)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .entity(exception.getMessage())
                .build();
    }
}