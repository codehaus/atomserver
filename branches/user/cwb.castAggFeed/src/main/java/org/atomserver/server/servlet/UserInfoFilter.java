/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
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

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;

/**
 * This servlet filter retrieves the principal name from HttpServletRequest and saves it in the thread local.
 * It is used to track which user makes the request in Perf4j logging.
 */
public class UserInfoFilter implements Filter
{
    public void init(FilterConfig filterConfig) throws ServletException {
         // no config
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        Principal principal = request.getUserPrincipal();
        if(principal != null) {
            String name = principal.getName();
            AtomServerUserInfo.setUser(name);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public void destroy() {
          // do nothing
    }

}
