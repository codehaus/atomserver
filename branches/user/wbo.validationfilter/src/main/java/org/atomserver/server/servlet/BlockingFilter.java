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

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.List;

/**
 * BlockingFilter - A servlet filter to check for blocked users, blocked URI paths,
 * and contents that are blocked based on their length. The order of checking is
 * content-length, user, and paths.
 *
 * HTTP STATUS CODES on blocked state
 * Blocked By               Status Code
 * Content Length > maximum    413  (Request entity too large)
 * User                        403  (Forbidden - the server understood the request but refused to fulfill it.)
 * Path                        403  (Forbidden - the server understood the request but refused to fulfill it.)
 */
public class BlockingFilter implements Filter
{
    // JMX MBean
    private final BlockingFilterSettings settings;

    /**
     * Constructor for BlockingFilter.
     * @param settings  <code>BlockingFilterSetting</code> object
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

        if(contentNotBlockedByLength(request, response) &&
           userNotBlocked(request, response) &&
           pathNotBlocked(request, response)) {
                filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    public void destroy() {
        // settings's life cyclce is maintained by spring.
    }

    private boolean contentNotBlockedByLength(final HttpServletRequest request, HttpServletResponse response)
    throws IOException {
        if((settings.getMaxContentLength() >= 0) &&
           (request.getContentLength() > settings.getMaxContentLength())) {
            response.sendError(javax.servlet.http.HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return false;
        }
        return true;
    }

    private boolean userNotBlocked(final HttpServletRequest request, HttpServletResponse response)
    throws IOException {
        String name;
        Principal principal = request.getUserPrincipal();
        if(principal != null) {
            name = principal.getName();
            if(settings.getBlockedUsers().contains(name)) {
                response.sendError(javax.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
        }
        return true;
    }

    private boolean pathNotBlocked(final HttpServletRequest request, HttpServletResponse response)
    throws IOException {
        List<String> paths = settings.getBlockedPaths();
        String uriPath = request.getRequestURI();
        for(String path: paths) {
            if(uriPath.matches(path))  {
                response.sendError(javax.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
        }
        return true;
    }
}
