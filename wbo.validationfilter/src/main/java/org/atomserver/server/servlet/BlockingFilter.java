/* Copyright (c) 2009 HomeAway, Inc.
 *  All rights reserved.  http://www.atomserver.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.atomserver.server.servlet;

import org.atomserver.exceptions.TooMuchDataException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;


import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.List;

/**
 * BlockingFilter - A servlet filter to check for blocked users, blocked URI paths,
 * and contents that are blocked based on their length. The order of checking is
 * content-length, user, and paths.
 * <p/>
 * HTTP STATUS CODES on blocked state
 * Blocked By                   Status Code
 * Content Length > maximum    413  (Request entity too large)
 * User                        403  (Forbidden - the server understood the request but refused to fulfill it.)
 * Path                        403  (Forbidden - the server understood the request but refused to fulfill it.)
 */
public class BlockingFilter implements Filter {
    static protected Log logger = LogFactory.getLog(BlockingFilter.class);

    // JMX MBean
    private final BlockingFilterSettings settings;

    /**
     * Constructor for BlockingFilter.
     *
     * @param settings <code>BlockingFilterSetting</code> object
     */
    public BlockingFilter(final BlockingFilterSettings settings) {
        this.settings = settings;
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // no config
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        if (contentNotBlockedByLength(request, response) &&
            userNotBlocked(request, response) &&
            pathNotBlocked(request, response)) {
            ServletRequest servRequest = (isContentLengthNotSet(request)) ? wrapServletRequest(request) : request;
            filterChain.doFilter(servRequest, servletResponse);
        }
    }

    public void destroy() {
        // settings's life cyclce is maintained by spring.
    }

    // Note: If the content length is -1, this method will return true.
    private boolean contentNotBlockedByLength(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        boolean isWrite = request.getMethod().equals("POST") || request.getMethod().equals("PUT");
        if (isWrite && settings.getMaxContentLength() >= 0) {
            if (request.getContentLength() > settings.getMaxContentLength()) {
                String message = "TOO MUCH DATA :: (Content length exceeds the maximum length allowed.) :: " +
                                 request.getRequestURI();
                setError(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, message);
                return false;
            }
        }
        return true;
    }

    // check if the content length is set to -1 on POST/PUT
    private boolean isContentLengthNotSet(final HttpServletRequest request) {
        boolean isWrite = request.getMethod().equals("POST") || request.getMethod().equals("PUT");
        return isWrite && (settings.getMaxContentLength() >= 0) && (request.getContentLength() == -1);
    }

    private boolean userNotBlocked(final HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String name;
        Principal principal = request.getUserPrincipal();
        if (principal != null) {
            name = principal.getName();
            if (settings.getBlockedUsers().contains(name)) {
                String message = "USER IS BLOCKED :: (" + name + " is blocked from accessing the server.) :: " +
                                 request.getRequestURI();
                setError(response, HttpServletResponse.SC_FORBIDDEN, message);
                return false;
            }
        }
        return true;
    }

    private boolean pathNotBlocked(final HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        List<String> paths = settings.getBlockedPaths();
        String uriPath = request.getRequestURI();
        for (String path : paths) {
            if (uriPath.matches(path)) {
                String message = "PATH IS BLOCKED :: (" + uriPath + " is blocked from access.) :: ";
                setError(response, HttpServletResponse.SC_FORBIDDEN, message);
                return false;
            }
        }
        return true;
    }

    /*
     * Wraps the servletResquest with tracking to validate its length.
     */
    private ServletRequest wrapServletRequest(ServletRequest servletRequest)
            throws IOException {
        final ServletInputStream originalInputStream = servletRequest.getInputStream();
        final int maxBytes = settings.getMaxContentLength();
        final ServletInputStream inputStream = new ServletInputStream() {
            int bytesRead = 0;

            public int read() throws IOException {
                if (bytesRead++ > maxBytes) {
                    throw new TooMuchDataException("Content length exceeds the maximum length allowed.");
                }
                return originalInputStream.read();
            }
        };
        // wrap the original request, returning the wrapped input stream instead
        return new HttpServletRequestWrapper((HttpServletRequest) servletRequest) {
            public ServletInputStream getInputStream() throws IOException {
                return inputStream;
            }
        };
    }

    /*
     * Set ServletResponse to abdera style xml response.
     */
    private void setError(HttpServletResponse response, int errCode, String message)
            throws IOException {
        logger.error(message);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/xml");
        response.setStatus(errCode);

        PrintWriter writer = response.getWriter();
        writer.println(errorMessage(errCode, message));
    }

    private String errorMessage(final int errCode, final String message) {
        return "<?xml version='1.0' encoding='UTF-8'?>" +
               "<error xmlns=\"http://incubator.apache.org/abdera\">" +
               "<code>" + errCode + "</code>" +
               "<message>" + message + "</message>" +
               "</error>";
    }
}
