/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.abdera.Abdera;
import org.apache.abdera.protocol.Resolver;
import org.apache.abdera.protocol.server.ServiceContext;
import org.apache.abdera.protocol.server.Target;
import org.apache.abdera.protocol.server.TargetType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.uri.URIHandler;
import org.atomserver.utils.conf.ConfigurationAwareClassLoader;
import org.atomserver.testutils.client.MockRequestContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class URITargetTestCase extends TestCase {

    static protected Log log = LogFactory.getLog( URITargetTestCase.class );

    public static Test suite()
    { return new TestSuite( RegexTargetResolverTest.class ); }

    static protected final String CONTEXT_NAME = "org.apache.abdera.protocol.server.ServiceContext";
    static protected String SERVER_URL ;

    protected Resolver<Target> targetResolver = null;
    protected ServiceContext serviceContext = null;

    protected String servletMapping;
    protected String servletContext;

    public void setUp() throws Exception {
        super.setUp();

        String[] configs = {"/org/atomserver/spring/propertyConfigurerBeans.xml",
                            "/org/atomserver/spring/databaseBeans.xml",
                            "/org/atomserver/spring/storageBeans.xml",
                            "/org/atomserver/spring/logBeans.xml",
                            "/org/atomserver/spring/abderaBeans.xml"};

        ClassPathXmlApplicationContext springFactory = new ClassPathXmlApplicationContext(configs, false);
        springFactory.setClassLoader( new ConfigurationAwareClassLoader(springFactory.getClassLoader()));
        springFactory.refresh();


        serviceContext = (ServiceContext)( springFactory.getBean( CONTEXT_NAME ) );
        if (serviceContext.getAbdera() == null) {
            serviceContext.init(new Abdera(), null );
        }
        assertNotNull( serviceContext.getAbdera() );

        URIHandler handler = ((AbstractAtomService) springFactory.getBean("org.atomserver-atomService")).getURIHandler();
         servletContext= handler.getServletContext();
        servletMapping = handler.getServletMapping();

        targetResolver = ( serviceContext.getTargetResolver( "/" + servletContext ) );
        assertNotNull( targetResolver );
        log.debug( "targetResolver= " + targetResolver );

        SERVER_URL = "http://localhost:12345/" + servletContext;
    }

    protected void tearDown() throws Exception
    { super.tearDown(); }
    protected Target checkTarget( String uri, TargetType expectedType ) {
        log.debug( "uri INPUT= " + uri );
        MockRequestContext requestContext = new MockRequestContext( serviceContext, "GET", uri );
        log.debug( "URI= " + requestContext.getTargetPath() );

        log.debug( "Calling targetResolver.resolve" );
        Target target = targetResolver.resolve( requestContext );
        assertNotNull( target );

        TargetType type = target.getType();
        log.debug( "TargetType = " + type );
        assertTrue( type.equals( expectedType ) );

        return target;
    }
        
}
