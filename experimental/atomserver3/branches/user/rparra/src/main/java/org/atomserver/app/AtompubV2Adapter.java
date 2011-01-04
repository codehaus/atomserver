package org.atomserver.app;

import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.*;

// This is the beginnings of an adapter that will "rewrite" requests against the /v2 api of
// AtomServer into /v3 requests.
//
// TODO: this should much more robust, maintainable, and tested.
public class AtompubV2Adapter implements Filter {

    private static final Logger log = Logger.getLogger(AtompubV2Adapter.class);

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @SuppressWarnings("unchecked")
	public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        log.debug(String.format("%s - %s", request.getMethod(), request.getRequestURI()));

        if (request.getRequestURI().contains("/v2/")) {
            final String originalUri = request.getRequestURI();
            final Map<String, Set<String>> headers = new HashMap<String, Set<String>>();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                Enumeration<String> headerValues = request.getHeaders(name);
                while (headerValues.hasMoreElements()) {
                    String value = headerValues.nextElement();
                    Set<String> values = headers.get(name);
                    if (values == null) {
                        headers.put(name, values = new HashSet<String>());
                    }
                    values.add(value);
                }
            }

            if (originalUri.endsWith("/*")) {
                headers.put("ETag", Collections.singleton("*"));
            }
            request = new HttpServletRequestWrapper(request) {
                String rewrittenUri = originalUri
                        .replaceFirst("/v2", "")
                        .replaceAll("/\\*$", "");

                public String getRequestURI() {
                    return rewrittenUri;
                }

                public String getHeader(String name) {
                    Enumeration<String> enumeration = getHeaders(name);
                    return enumeration.hasMoreElements() ? (String) enumeration.nextElement() : null;
                }

                public Enumeration<String> getHeaderNames() {
                    return Collections.enumeration(headers.keySet());
                }

                public Enumeration<String> getHeaders(String name) {
                    Set<String> headerValues = headers.get(name);
                    return Collections.enumeration(
                            headerValues == null ? Collections.EMPTY_SET : headerValues);
                }
            };

            log.debug(String.format("rewrote %s to %s", originalUri, request.getRequestURI()));
        }

        chain.doFilter(request, servletResponse);
    }


    public void destroy() {
    }
}
