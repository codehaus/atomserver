/* Copyright (c) 2007 HomeAway, Inc.
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

import org.apache.abdera.protocol.server.ServiceContext;
import org.apache.abdera.protocol.server.servlet.AbderaServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * The AtomServer Servlet.
 * Note that this Servlet doesn't do much. It immediately delegates to the Abdera RequestHandler
 * for processing the Servlet Request. The reason we use it instead of the AbderaServlet
 * is to enable Spring wiring.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class AtomServerServlet
        extends AbderaServlet {

    private final static Log logger = LogFactory.getLog(AtomServerServlet.class);

    public static final String SPRING_APPLICATION_CONTEXT_FILE = "springAppContextFile";

    private ApplicationContext appContext;

    @Override
    public void init(ServletConfig config)
            throws ServletException {
        super.init(config);

        loadSpringContext();
    }

    protected void loadSpringContext() {
        if (appContext == null) {
            ServletConfig config = getServletConfig();

            ServletContext context = config.getServletContext();
            if (context != null) {
                logger.debug("LOADING: WebApplicationContextUtils.getRequiredWebApplicationContext(context))");
                appContext = WebApplicationContextUtils.getRequiredWebApplicationContext(context);
            } else {
                logger.debug("LOADING: new ClassPathXmlApplicationContext( .... )");
                appContext = new ClassPathXmlApplicationContext(config.getInitParameter(SPRING_APPLICATION_CONTEXT_FILE));
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Application context set:: appContext= " + appContext);
            }
        }
    }

    protected ServiceContext createServiceContext() {
        String contextName = getInitParameter("serviceContextBeanName");
        if (contextName == null) {
            contextName = ServiceContext.class.getName();
        }

        loadSpringContext();
        logger.debug("createServiceContext() LOADING: loadSpringContext() : appContext= " + appContext);

        ServiceContext sc = (ServiceContext) appContext.getBean(contextName);
        sc.init(getAbdera(), getProperties(getServletConfig()));
        return sc;
    }

    protected ApplicationContext getApplicationContext() {
        return appContext;
    }
}
