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

package org.atomserver.utils.jetty;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.core.dbstore.utils.DBSeeder;
import org.atomserver.utils.conf.ConfigurationAwareClassLoader;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.File;
import java.util.Properties;

/**
 * StandAloneJettyServer - A standalone Jetty Server for simple, possibly zero configuration start up.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class StandAloneAtomServer {
    static private Log log = LogFactory.getLog(StandAloneAtomServer.class);
    static private final String DEFAULT_VERSIONS_FILE = "version.properties";
    static private final int DEFAULT_PORT = 7890;

    public static void main(String[] args)
            throws Exception {

        // the System Property "atomserver.home" defines the home directory for the standalone app
        String atomserverHome = System.getProperty("atomserver.home");
        if (StringUtils.isEmpty(atomserverHome)) {
            log.error("The variable \"atomserver.home\" must be defined");
            System.exit(-1);
        }
        File atomserverHomeDir = new File(atomserverHome);
        if (!atomserverHomeDir.exists() && !atomserverHomeDir.isDirectory()) {
            log.error("The variable \"atomserver.home\" (" +
                      atomserverHome + ") must point to a directory that exists");
        }

        // instantiate the Jetty Server
        Server server = new Server();

        // create a new connector on the declared port, and set it onto the server
        log.debug("atomserver.port = " + System.getProperty("atomserver.port"));
        Connector connector = new SelectChannelConnector();
        connector.setPort(Integer.getInteger("atomserver.port", DEFAULT_PORT));
        server.setConnectors(new Connector[]{connector});

        // create a ClassLoader that points at the conf directories
        log.debug("atomserver.conf.dir = " + System.getProperty("atomserver.conf.dir"));
        log.debug("atomserver.ops.conf.dir = " + System.getProperty("atomserver.ops.conf.dir"));
        ClassLoader classLoader = new ConfigurationAwareClassLoader(
                StandAloneAtomServer.class.getClassLoader());

        // load the version from the version.properties file
        Properties versionProps = new Properties();
        versionProps.load(classLoader.getResourceAsStream(DEFAULT_VERSIONS_FILE));
        String version = versionProps.getProperty("atomserver.version");

        // create a new webapp, rooted at webapps/atomserver-${version}, with the configured
        // context name
        String servletContextName = System.getProperty("atomserver.servlet.context");
        log.debug("atomserver.servlet.context = " + servletContextName);
        WebAppContext webapp =
                new WebAppContext(atomserverHome + "/webapps/atomserver-" + version,
                                  "/" + servletContextName);

        // set the webapp's ClassLoader to be the one that loaded THIS class.  the REASON that
        // this needs to be set is so that when we extract the web application context below we can
        // cast it to WebApplicationContext here
        webapp.setClassLoader(StandAloneAtomServer.class.getClassLoader());

        // set the Jetty server's webapp and start it
        server.setHandler(webapp);
        server.start();

        // if the seed system property was set, use the DBSeeder to populate the server
        String seedDB = System.getProperty("seed.database.with.pets");
        log.debug("seed.database.with.pets = " + seedDB);
        if (!StringUtils.isEmpty(seedDB)) {
            if (Boolean.valueOf(seedDB)) {
                Thread.sleep(2000);

                WebApplicationContext webappContext =
                        WebApplicationContextUtils.getWebApplicationContext(webapp.getServletContext());

                DBSeeder.getInstance(webappContext).seedPets();
            }
        }

        server.join();
    }
}
