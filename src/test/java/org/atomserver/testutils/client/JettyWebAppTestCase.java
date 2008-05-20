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

package org.atomserver.testutils.client;

import junit.framework.TestCase;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.webapp.WebAppContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.atomserver.utils.conf.ConfigurationAwareClassLoader;

import javax.servlet.ServletContext;
import java.util.Random;


/**
 * This base JUnit TestCase starts up Jetty based on "spring/jettyBeans.xml"
 * It does this by using a <b>separate</b> Spring ApplicationContext from that
 * used by the webapp itself. I.e. separate from the WebApplicationContext
 * used for our Spring-based Abdera Servlet.
 * <p/>
 * You specify whether you want either a non-secure or a secure system
 * in Class constructor. This turns off/on the Acegi Servlet Filters
 */
public class JettyWebAppTestCase extends TestCase {

    protected Log log = LogFactory.getLog(JettyWebAppTestCase.class);

    // Create a psuedo-random number generator  
    static private Random random = new Random();

    static private final int NOT_SET = -1;
    static private final String JETTY_CONNECTOR_ID = "httpConnector";

    static protected String[] DEFAULT_CONFIGS = {"/org/atomserver/spring/propertyConfigurerBeans.xml",
                                                 "/jettyBeans.xml"};

    static protected String jettyServerBeanId = "jettyWebAppServer";


    protected Server jettyServer = null;

    private ServletHandler servletHandler = null;

    protected String servletContextName;

    protected ClassPathXmlApplicationContext jettySpringFactory = null;
    protected ApplicationContext appSpringFactory = null;

    private int jettyPort = NOT_SET;

    private boolean useNonSecure = true;
    private String[] configs = null;

    protected JettyWebAppTestCase() {
        this(true);
    }

    protected JettyWebAppTestCase(boolean useNonSecure) {
        this(useNonSecure, DEFAULT_CONFIGS);
    }

    protected JettyWebAppTestCase(boolean useNonSecure, String[] configs) {
        this.useNonSecure = useNonSecure;
        this.configs = configs;
    }

    protected String getServletContextName() {
        return servletContextName;
    }

    protected void setUp() throws Exception {
        try {
            super.setUp();

            // just in case, stop Jetty if it is running
            stopJetty();

            // Set the variable in the Spring acegiBeans.xml
            log.debug("useNonSecure= " + useNonSecure);
            if (useNonSecure) {
                System.setProperty("security.use.filters", "false");
            } else {
                System.setProperty("security.use.filters", "true");
            }

            if (jettySpringFactory == null) {
                log.debug( "Loading jettySpringFactory........");
                jettySpringFactory = new ClassPathXmlApplicationContext(configs, false);
                jettySpringFactory.setClassLoader(
                            new ConfigurationAwareClassLoader(jettySpringFactory.getClassLoader()));
                jettySpringFactory.refresh();
            }

            startJetty();

        } catch (Exception ee) {
            // just in case, stop Jetty if it is running
            stopJetty();
            throw ee;
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        // set it back to the default
        System.setProperty("security.use.filters", "true");

        stopJetty();
    }

    /**
     * NOTE: if you're getting BindExceptions (Address is in use)
     * This can happen if an Exception is thrown during the setUp()
     * And, in general, this will be due to some errant file in /var
     * So try a ./bin/clear-test-files.sh.
     */
    private void startJetty() throws Exception {
        Server jserver = getJettyServer();
        if (!jserver.isStarted()) {
            try {
                doStart(jserver);
            } catch (org.mortbay.util.MultiException ee) {
                startJettyOnAlternatePorts(jserver);
            } catch (java.net.BindException ee) {
                startJettyOnAlternatePorts(jserver);
            }
        }
    }

    private void doStart(Server jserver) throws Exception {
        log.debug("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        log.debug(getPortsString());
        log.debug("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        jserver.start();
    }

    private int getPortAdd() {
        int portAdd = 0;
        while (portAdd == 0) {
            portAdd = random.nextInt(1000);
        }
        return portAdd;
    }

    private void startJettyOnAlternatePorts(Server jserver) throws Exception {
        log.debug("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        log.debug("              TRYING ALTERNATE PORTS ");
        log.debug("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        int portNS = getPort();
        int tries = 0;
        while (tries < 3) {
            try {
                tries++;
                int portAdd = getPortAdd();
                setPort(portNS + portAdd, JETTY_CONNECTOR_ID);
                doStart(jserver);
                break;
            } catch (org.mortbay.util.MultiException ee) {
                // continue...
            } catch (java.net.BindException ee) {
                // continue...
            }
        }
    }

    private void stopJetty() throws Exception {
        if (jettySpringFactory != null) {
            Server jserver = getJettyServer();
            if (jserver != null && jserver.isStarted()) {
                log.debug("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                log.debug(getPortsString());
                log.debug("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                jserver.stop();
            }
        }
    }

    private String getPortsString() {
        return "       START JETTY ( port: " + getPort() + ")";
    }


    /**
     * return the Application Spring WebApplicationContext.
     * Its a bit convoluted to get at it from here, but ultimately its doable.
     * We have to traverse from the Jetty Server, until we get to the ServletContext.
     * NOTE: this code is entirely dependent on the structure of jettyBeans.xml
     * Thus, if that Bean changes, so must this code !!!
     */
    protected ApplicationContext getSpringFactory() throws Exception {
        if (appSpringFactory == null) {
            Server jserver = getJettyServer();

            WebAppContext webappContext = (WebAppContext) jserver.getHandler();
            log.debug("webappContext= " + webappContext);

            servletHandler = webappContext.getServletHandler();
            log.debug("ServletHandler = " + servletHandler);

            ServletContext servletContext = servletHandler.getServletContext();
            log.debug("servletContext= " + servletContext);

            servletContextName = servletContext.getServletContextName();
            if (servletContextName == null) {
                throw new RuntimeException("No servletContextName defined for atomserver");
            }
            // will come back as e.g. /atomserver/
            servletContextName = StringUtils.strip(servletContextName, "/");
            log.debug("****************** servletContextName= " + servletContextName);

            appSpringFactory =
                    WebApplicationContextUtils.getWebApplicationContext(servletContext);
        }
        return appSpringFactory;
    }

    protected Server getJettyServer() {
        if (jettyServer == null) {
            jettyServer = (Server) jettySpringFactory.getBean(jettyServerBeanId);
            assertNotNull(jettyServer);
        }
        return jettyServer;
    }

    protected ServletHandler getJettyServletHandler() throws Exception {
        if (appSpringFactory == null) {
            getSpringFactory();
        }
        return servletHandler;
    }

    private void setPort(int port, String connectorName) {
        log.debug("Setting port for " + connectorName + " to " + port);
        Connector conn = getConnector(connectorName);
        conn.setPort(port);
        if (connectorName.equals(JETTY_CONNECTOR_ID)) {
            jettyPort = port;
        }
    }

    protected int getPort() {
        if (jettyPort == NOT_SET) {
            jettyPort = getPortFromConnector(JETTY_CONNECTOR_ID);
        }
        return jettyPort;
    }

    private int getPortFromConnector(String connectorName) {
        Connector conn = getConnector(connectorName);
        int port = conn.getPort();
        assertTrue(port != NOT_SET);
        return port;
    }

    private Connector getConnector(String connectorName) {
        Server jserver = getJettyServer();
        Connector[] connectors = jserver.getConnectors();
        for (int ii = 0; ii < connectors.length; ii++) {
            log.debug("Connector Name = " + connectors[ii].getName());
            if (connectors[ii].getName().equals(connectorName)) {
                return connectors[ii];
            }
        }
        fail("The Connector named (" + connectorName + ") was not found in the Jetty Spring Bean");
        return null;
    }
}
