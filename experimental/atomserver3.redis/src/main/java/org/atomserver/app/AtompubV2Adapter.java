package org.atomserver.app;

import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

public class AtompubV2Adapter implements Filter {

    private static final Logger log = Logger.getLogger(AtompubV2Adapter.class);

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        log.debug(String.format("%s - %s", request.getMethod(), request.getRequestURI()));

        if (request.getRequestURI().startsWith("/app/v2")) {
            String originalUri = request.getRequestURI();
            request = new HttpServletRequestWrapper(request) {
                String rewrittenUri = super.getRequestURI()
                        .replaceFirst("^/app/v2", "/app")
                        .replaceAll("/\\*$", "");

                public String getRequestURI() {
                    return rewrittenUri;
                }
            };

            log.debug(String.format("rewrote %s to %s", originalUri, request.getRequestURI()));
        }

        chain.doFilter(request, servletResponse);
    }


    public void destroy() {
    }
}
