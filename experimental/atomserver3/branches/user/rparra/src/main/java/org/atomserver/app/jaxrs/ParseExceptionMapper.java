package org.atomserver.app.jaxrs;

import org.apache.abdera.parser.ParseException;
import org.apache.log4j.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ParseExceptionMapper implements ExceptionMapper<ParseException> {

    private static final Logger log = Logger.getLogger(ParseExceptionMapper.class);
    public static final String MESSAGE = "Unable to parse a valid object from request entity.";

    public Response toResponse(ParseException exception) {
        log.warn(MESSAGE, exception);
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(MESSAGE)
                .build();
    }
}
