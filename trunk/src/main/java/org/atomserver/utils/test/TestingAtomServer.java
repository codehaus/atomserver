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
package org.atomserver.utils.test;

import org.apache.abdera.Abdera;
import org.apache.abdera.protocol.server.ServiceContext;
import org.atomserver.core.WorkspaceOptions;
import org.atomserver.core.autotaggers.XPathAutoTagger;
import org.atomserver.core.dbstore.DBBasedContentStorage;
import org.atomserver.core.validators.RelaxNGValidator;
import org.atomserver.server.servlet.AtomServerServlet;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;

/**
 * TestingAtomServer - a tiny standalone AtomServer instance, for unit testing AtomServer clients.
 * <p/>
 * This class brings up an instance of AtomServer using an embedded Jetty server and an in-memory
 * HSQL DB.  It provides a simple API for configuring workspaces, or allows you to specify the
 * location on the classpath of a Spring XML file to configure any atomserver beans, including,
 * but not limited to, workspaces.
 * <p/>
 * usage:
 * <pre>
 *  TestingAtomServer server = new TestingAtomServer();
 *  server.addWorkspace("foo", true);           // add workspace "foo", which is localized
 *  server.start(9000, "atomserver", "v1");     // start at http://localhost:9000/atomserver/v1
 * <p/>
 *  // do your testing here
 * <p/>
 *  server.stop();
 * </pre>
 */
public class TestingAtomServer {
    private Server httpServer;
    private int port;

    public int start(String atomserverServletContext, String atomserverServletMapping) throws Exception {
        if (this.httpServer != null) {
            throw new IllegalStateException("Must stop server before calling start");
        }

        for (int i = 40000; i < 41000; i++) {
            try {
                this.httpServer = createServer(this.port = i,
                                               atomserverServletContext,
                                               atomserverServletMapping);
                this.httpServer.start();
                return this.port;
            } catch (Exception e) {
                System.out.println("Couldn't start port on " + i + ", trying next port");
            }
        }

        throw new RuntimeException("Couldn't find open port");
    }

    public void start(int port,
                      String atomserverServletContext,
                      String atomserverServletMapping) throws Exception {
        if (this.httpServer != null) {
            throw new IllegalStateException("Must stop server before calling start");
        }

        this.httpServer = createServer(this.port = port,
                                       atomserverServletContext,
                                       atomserverServletMapping);
        this.httpServer.start();
    }

    public void stop() throws Exception {
        if (this.httpServer != null) {
            this.httpServer.stop();
            this.httpServer.join();
            this.httpServer = null;
            this.port = 0;
        }
    }

    private String springBeansLocation = null;
    private ManagedSet workspaceSet = new ManagedSet();

    /**
     * set the location on the classpath of a spring XML file to load.
     *
     * @param springBeansLocation the location on the classpath of a spring XML file
     */
    public void setSpringBeansLocation(String springBeansLocation) {
        this.springBeansLocation = springBeansLocation;
    }

    /**
     * A simple "chaining" API that lets the caller configure additional parameters of the workspaces.
     */
    public interface TestWorkspaceConfigurer {
        TestWorkspaceConfigurer setRncLocation(String rncLocation);

        TestWorkspaceConfigurer setXPathAutotaggerRules(String rules);

        TestWorkspaceConfigurer addPropertyValue(String name, Object value);
    }

    /**
     * add a new workspace to the testing server with the given name and localization flag.
     *
     * @param name      the name of the workspace
     * @param localized true iff the workspace is localized
     * @return a TestWorkspaceConfigurer to further configure the workspace
     */
    public TestWorkspaceConfigurer addWorkspace(String name, boolean localized) {
        // create a bean definition for the workspace and add it to the managed set.
        final RootBeanDefinition workspace = new RootBeanDefinition(WorkspaceOptions.class);

        MutablePropertyValues propertyValues = new MutablePropertyValues();
        propertyValues.addPropertyValue("name", name);
        propertyValues.addPropertyValue("defaultLocalized", localized);
        propertyValues.addPropertyValue("defaultProducingEntryCategoriesFeedElement", true);
        propertyValues.addPropertyValue("defaultContentStorage",
                                        new RuntimeBeanReference("org.atomserver-contentStorage"));
        propertyValues.addPropertyValue("defaultContentValidator",
                                        new RuntimeBeanReference("org.atomserver-simpleXMLContentValidator"));
        propertyValues.addPropertyValue("defaultCategoriesHandler",
                                        new RuntimeBeanReference("org.atomserver-entryCategoriesHandler"));
        propertyValues.addPropertyValue("defaultEntryIdGenerator",
                                        new RuntimeBeanReference("org.atomserver-entryIdGenerator"));

        workspace.setPropertyValues(propertyValues);

        workspaceSet.add(workspace);

        return new TestWorkspaceConfigurer() {
            // if given a RNC location, we spin up a RelaxNGValidator to validate with it
            public TestWorkspaceConfigurer setRncLocation(String rncLocation) {
                RootBeanDefinition autotagger = new RootBeanDefinition(RelaxNGValidator.class);
                MutablePropertyValues propertyValues = new MutablePropertyValues();
                propertyValues.addPropertyValue("schemaLocation", rncLocation);
                autotagger.setPropertyValues(propertyValues);
                workspace.getPropertyValues().addPropertyValue("defaultContentValidator", autotagger);
                return this;
            }

            // if given an XPathAutoTagger script, set up a tagger to use it
            public TestWorkspaceConfigurer setXPathAutotaggerRules(String rules) {
                RootBeanDefinition autotagger = new RootBeanDefinition(XPathAutoTagger.class);
                MutablePropertyValues propertyValues = new MutablePropertyValues();
                propertyValues.addPropertyValue("performanceLog",
                                                new RuntimeBeanReference("org.atomserver-performanceLog"));
                propertyValues.addPropertyValue("entryCategoriesDAO",
                                                new RuntimeBeanReference("org.atomserver-entryCategoriesDAO"));
                propertyValues.addPropertyValue("script", rules);
                autotagger.setPropertyValues(propertyValues);
                workspace.getPropertyValues().addPropertyValue("defaultAutoTagger", autotagger);
                return this;
            }

            // allow for arbitrary property setting on the workspace bean -- requires knowledge
            // of the Spring bean factory APIs
            public TestWorkspaceConfigurer addPropertyValue(String name, Object value) {
                workspace.getPropertyValues().addPropertyValue(name, value);
                return this;
            }
        };
    }

    private Server createServer(int port, String atomserverServletContext, String atomserverServletMapping) {
        // set up the server and the atomserver web application context
        Server server = new Server(port);
        Context context = new Context(server, "/" + atomserverServletContext,
                                      true /*sessions*/, false /*no security*/);

        // we need to hard-code certain system properties to get the behavior we want here - we
        // will re-set them when we're done
        Properties properties = (Properties) System.getProperties().clone();
        System.setProperty("atomserver.env", "asdev-hsql-mem");
        System.setProperty("atomserver.servlet.context", atomserverServletContext);
        System.setProperty("atomserver.servlet.mapping", atomserverServletMapping);

        // our Spring application context will start off by loading the basic built-in bean
        // definitions
        final GenericWebApplicationContext appContext = new GenericWebApplicationContext();
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(appContext);
        xmlReader.loadBeanDefinitions(new ClassPathResource("org/atomserver/spring/propertyConfigurerBeans.xml"));
        xmlReader.loadBeanDefinitions(new ClassPathResource("org/atomserver/spring/databaseBeans.xml"));
        xmlReader.loadBeanDefinitions(new ClassPathResource("org/atomserver/spring/logBeans.xml"));
        xmlReader.loadBeanDefinitions(new ClassPathResource("org/atomserver/spring/storageBeans.xml"));
        xmlReader.loadBeanDefinitions(new ClassPathResource("org/atomserver/spring/abderaBeans.xml"));

        // if we were given a Spring config location, we use that -- otherwise we configure the
        // workspaces that were set up through the API
        if (springBeansLocation != null) {
            xmlReader.loadBeanDefinitions(new ClassPathResource(springBeansLocation));
        } else {
            RootBeanDefinition workspaces = new RootBeanDefinition(HashSet.class);
            ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
            constructorArgumentValues.addGenericArgumentValue(workspaceSet);
            workspaces.setConstructorArgumentValues(constructorArgumentValues);

            appContext.registerBeanDefinition("org.atomserver-workspaces", workspaces);
        }

        // override the base content storage to use DB-based storage
        RootBeanDefinition storage = new RootBeanDefinition(DBBasedContentStorage.class);
        MutablePropertyValues propertyValues = new MutablePropertyValues();
        propertyValues.addPropertyValue("contentDAO",
                                        new RuntimeBeanReference("org.atomserver-contentDAO"));
        propertyValues.addPropertyValue("entriesDAO",
                                        new RuntimeBeanReference("org.atomserver-entriesDAO"));
        storage.setPropertyValues(propertyValues);
        appContext.registerBeanDefinition("org.atomserver-contentStorage", storage);

        // refresh the context to actually instantiate everything.
        appContext.refresh();

        // re-set the system properties
        System.setProperties(properties);

        // put our app context into the servlet context
        context.setAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
                appContext);

        // load and init the service context
        String contextName = context.getInitParameter("serviceContextBeanName");
        final ServiceContext serviceContext =
                (ServiceContext) appContext.getBean(
                        contextName == null ? ServiceContext.class.getName() : contextName);
        serviceContext.init(new Abdera(), Collections.EMPTY_MAP);

        // create a new AtomServerServlet - but override the createServiceContext method
        AtomServerServlet servlet = new AtomServerServlet() {
            protected ServiceContext createServiceContext() {
                return serviceContext;
            }
        };

        // register the servlet
        context.addServlet(new ServletHolder(servlet), "/" + atomserverServletMapping + "/*");

        // ready to be started
        return server;
    }


}
