package org.atomserver.app.jaxrs;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import org.apache.log4j.Logger;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;

@Component
public class AroundJaxrsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger perf = Logger.getLogger(StopWatch.DEFAULT_LOGGER_NAME);

    private final ThreadLocal<StopWatch> stopwatch = new ThreadLocal<StopWatch>();

    public ContainerRequest filter(ContainerRequest request) {
        stopwatch.set(new Log4JStopWatch(perf));
        return request;
    }

    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        StringBuilder tag = new StringBuilder();
        for (Annotation annotation : response.getAnnotations()) {
            if (annotation instanceof GET) {
                tag.append(request.getMethod()).append(".").append(((GET)annotation).value());
            } else if (annotation instanceof PUT) {
                tag.append(request.getMethod()).append(".").append(((PUT)annotation).value());
            }else if (annotation instanceof DELETE) {
                tag.append(request.getMethod()).append(".").append(((DELETE)annotation).value());
            }else if (annotation instanceof POST) {
                tag.append(request.getMethod()).append(".").append(((POST)annotation).value());
            }else if (annotation instanceof HEAD) {
                tag.append(request.getMethod()).append(".").append(((HEAD)annotation).value());
            }
        }
        stopwatch.get().stop(tag.toString());
        return response;
    }
}
