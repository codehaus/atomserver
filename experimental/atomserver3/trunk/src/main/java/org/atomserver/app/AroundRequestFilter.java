package org.atomserver.app;

import org.apache.log4j.Logger;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class AroundRequestFilter implements Filter {

    private static final Logger perf = Logger.getLogger(StopWatch.DEFAULT_LOGGER_NAME);

    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final StopWatch stopwatch = new Log4JStopWatch(perf);

        try {
            chain.doFilter(servletRequest, servletResponse);
        } finally {
            stopwatch.stop(String.format("%s.HTTP", request.getMethod()));
        }
    }

    public void destroy() {
    }
}
