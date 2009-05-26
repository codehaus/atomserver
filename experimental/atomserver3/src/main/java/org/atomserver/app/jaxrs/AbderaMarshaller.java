package org.atomserver.app.jaxrs;

import org.apache.abdera.model.ExtensibleElement;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.stax.FOMFactory;
import org.apache.abdera.parser.stax.FOMParser;
import org.apache.commons.io.IOUtils;
import static org.atomserver.AtomServerConstants.APPLICATION_APP_XML;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import static javax.ws.rs.core.MediaType.*;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Produces({APPLICATION_ATOM_XML, APPLICATION_APP_XML, APPLICATION_XML, TEXT_XML})
@Consumes({APPLICATION_ATOM_XML, APPLICATION_APP_XML, APPLICATION_XML, TEXT_XML})
public class AbderaMarshaller implements MessageBodyWriter, MessageBodyReader {

    public void writeTo(Object o, Class aClass, Type type, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap multivaluedMap,
                        OutputStream outputStream)
            throws IOException, WebApplicationException {

        ((ExtensibleElement) o).writeTo(outputStream);
    }

    public Object readFrom(Class aClass, Type type, Annotation[] annotations,
                           MediaType mediaType, MultivaluedMap multivaluedMap,
                           InputStream inputStream)
            throws IOException, WebApplicationException {
        // parse doesn't completely drain the InputStream before returning, so for large enough
        // documents, we were getting a "stream is closed" exception while traversing the object.
        // by draining the stream to a byte array and parsing from the ByteArrayInputStream, we
        // ensure that won't happen.
        byte[] bytes = IOUtils.toByteArray(inputStream);
        if (bytes.length == 0) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("Empty request body is not valid for this request.")
                            .build());
        }
        return parser().parse(new ByteArrayInputStream(bytes)).getRoot();
    }

    public boolean isWriteable(Class aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return isAbderaExtensibleElement(aClass);
    }

    public long getSize(Object o, Class aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    public boolean isReadable(Class aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return isAbderaExtensibleElement(aClass);
    }

    private boolean isAbderaExtensibleElement(Class clazz) {
        return ExtensibleElement.class.isAssignableFrom((Class<?>) clazz);
    }

    private static final transient ThreadLocal<Parser> PARSER = new ThreadLocal<Parser>() {
        protected Parser initialValue() {
            return new FOMParser();
        }
    };

    public static Parser parser() {
        return PARSER.get();
    }

    private static final transient ThreadLocal<FOMFactory> FACTORY =
            new ThreadLocal<FOMFactory>() {
                protected FOMFactory initialValue() {
                    return new FOMFactory();
                }
            };

    public static FOMFactory factory() {
        return FACTORY.get();
    }

}