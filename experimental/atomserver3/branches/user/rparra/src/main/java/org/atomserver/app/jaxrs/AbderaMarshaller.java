package org.atomserver.app.jaxrs;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.ExtensibleElement;
import org.apache.commons.io.IOUtils;
import org.atomserver.app.BadRequestException;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static javax.ws.rs.core.MediaType.*;
import static org.atomserver.AtomServerConstants.APPLICATION_APP_XML;

@Provider
@Component
@Produces({APPLICATION_ATOM_XML, APPLICATION_APP_XML, APPLICATION_XML, TEXT_XML})
@Consumes({APPLICATION_ATOM_XML, APPLICATION_APP_XML, APPLICATION_XML, TEXT_XML})
public class AbderaMarshaller
        implements MessageBodyWriter<ExtensibleElement>, MessageBodyReader<ExtensibleElement> {
    public static final Abdera ABDERA = new Abdera();

    public void writeTo(ExtensibleElement element, Class clazz, Type type,
                        Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap multivaluedMap, OutputStream outputStream)
            throws IOException, WebApplicationException {

        element.writeTo(outputStream);
    }

    public ExtensibleElement readFrom(Class clazz, Type type, Annotation[] annotations,
                                      MediaType mediaType, MultivaluedMap multivaluedMap,
                                      InputStream inputStream)
            throws IOException, WebApplicationException {
        // parse doesn't completely drain the InputStream before returning, so for large enough
        // documents, we were getting a "stream is closed" exception while traversing the object.
        // by draining the stream to a byte array and parsing from the ByteArrayInputStream, we
        // ensure that won't happen.
        byte[] bytes = IOUtils.toByteArray(inputStream);
        if (bytes.length == 0) {
            throw new BadRequestException("Empty request body is not valid for this request.");
        }
        return ABDERA.getParser().<ExtensibleElement>parse(
                new ByteArrayInputStream(bytes)).getRoot();
    }

    public boolean isWriteable(Class clazz, Type type,
                               Annotation[] annotations, MediaType mediaType) {
        return isAbderaExtensibleElement(clazz);
    }

    public long getSize(ExtensibleElement element, Class clazz, Type type,
                        Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    public boolean isReadable(Class clazz, Type type,
                              Annotation[] annotations, MediaType mediaType) {
        return isAbderaExtensibleElement(clazz);
    }

    private boolean isAbderaExtensibleElement(Class clazz) {
        return ExtensibleElement.class.isAssignableFrom((Class<?>) clazz);
    }
}