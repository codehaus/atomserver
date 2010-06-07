package org.atomserver.app.jaxrs;

import org.apache.log4j.Logger;
import org.atomserver.app.AtompubException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AtompubExceptionMapper implements ExceptionMapper<AtompubException> {

    private static final Logger log = Logger.getLogger(AtompubExceptionMapper.class);

    public Response toResponse(AtompubException exception) {
        switch (exception.status.getFamily()) {
            case CLIENT_ERROR:
                log.warn(exception.getMessage(), exception);
                break;
            case SERVER_ERROR:
                log.error(exception.getMessage(), exception);
                break;
        }
        
        return Response.status(exception.status)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .entity(exception.getMessage())
                .build();
    }
}