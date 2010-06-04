package org.atomserver.app.jaxrs;

import org.apache.abdera.parser.ParseException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ParseExceptionMapper implements ExceptionMapper<ParseException> {

    public Response toResponse(ParseException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity("Unable to parse a valid object from request entity.")
                .build();
    }
}
