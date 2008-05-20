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

package org.atomserver.core;

import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.uri.URIHandler;
import org.atomserver.utils.conf.ConfigurationAwareClassLoader;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class RegexTargetResolverTest extends TestCase {

    static private Log log = LogFactory.getLog( RegexTargetResolverTest.class );

    public static Test suite() 
    { return new TestSuite( RegexTargetResolverTest.class ); }
    
    static private final String CONTEXT_NAME = "org.apache.abdera.protocol.server.ServiceContext";
    static private String SERVER_URL ;

    private Resolver<Target> targetResolver = null;
    private ServiceContext serviceContext = null;

    private String servletMapping;
    private String servletContext;

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
    
    //----------------------------
    //          Tests 
    //----------------------------
    public void testService() throws Exception {
        checkRegex( "/" + servletMapping + "/",  TargetType.TYPE_SERVICE );
        checkRegex( "/" + servletMapping + "/widgets",  TargetType.TYPE_SERVICE );
        checkRegex( "/" + servletMapping + "/widgets/",  TargetType.TYPE_SERVICE );
    }

    public void testCollection() throws Exception {
        checkRegex( "/" + servletMapping + "/widgets/acme",  TargetType.TYPE_COLLECTION );
        checkRegex( "/" + servletMapping + "/widgets/acme/",  TargetType.TYPE_COLLECTION );
        checkRegex( "/" + servletMapping + "/widgets/acme?xyz=foo&abc=bar",  TargetType.TYPE_COLLECTION );
        checkRegex( "/" + servletMapping + "/widgets/acme?updated-min=2007-10-09T22:42:26.000Z&locale=en",  TargetType.TYPE_COLLECTION );
        checkRegex( "/" + servletMapping + "/widgets/acme?xyz=foo&abc=bar&lmn=goop",  TargetType.TYPE_COLLECTION );

        checkRegex( "/" + servletMapping + "/widgets/acme/-/cat1/cat2",  TargetType.TYPE_COLLECTION );
        checkRegex( "/" + servletMapping + "/widgets/acme/-/cat1",  TargetType.TYPE_COLLECTION );
        checkRegex( "/" + servletMapping + "/widgets/acme/-/cat1?xyz=foo&abc=bar&lmn=goop",  TargetType.TYPE_COLLECTION );
        checkRegex( "/" + servletMapping + "/widgets/acme/-/cat1?updated-min=2007-10-09T22:42:26.000Z&locale=en",  TargetType.TYPE_COLLECTION );
        checkRegex( "/" + servletMapping + "/widgets/acme/-/cat1/cat2/cat3",  TargetType.TYPE_COLLECTION );
        checkRegex( "/" + servletMapping + "/widgets/acme/-/cat1/cat2/cat3?xyz=foo&abc=bar&lmn=goop",  TargetType.TYPE_COLLECTION );
        checkRegex( "/" + servletMapping + "/widgets/acme/-/cat1/cat2/cat3?updated-min=2007-10-09T22:42:26.000Z&locale=en",  TargetType.TYPE_COLLECTION );
        checkRegex( "/" + servletMapping + "/widgets/acme/-/(urn:widgets.foo)test2",  TargetType.TYPE_COLLECTION );
        checkRegex( "/" + servletMapping + "/widgets/acme/-/(urn:widgets.foo)test0/(urn:widgets.foo)boo0",  TargetType.TYPE_COLLECTION );
        checkRegex( "/" + servletMapping + "/widgets/acme/-/(urn:widgets.foo)test2/(urn:widgets.foo)boo1/(urn:widgets.foo)ugh1",  TargetType.TYPE_COLLECTION );
        checkRegex( "/" + servletMapping + "/widgets/acme/-/(urn:widgets.foo)test2/(urn:widgets.foo)boo1/(urn:widgets.foo)ugh1/(urn:widgets.foo)oops",  TargetType.TYPE_COLLECTION );
    }

    public void testEntry() throws Exception {
        checkRegex( "/" + servletMapping + "/widgets/acme/123.en.xml",  TargetType.TYPE_ENTRY );
        checkRegex( "/" + servletMapping + "/widgets/acme/123",  TargetType.TYPE_ENTRY );
        checkRegex( "/" + servletMapping + "/widgets/acme/123?xyz=foo&abc=bar",  TargetType.TYPE_ENTRY );
        checkRegex( "/" + servletMapping + "/widgets/acme/123?xyz=foo&abc=bar&lmn=goop",  TargetType.TYPE_ENTRY );
        checkRegex( "/" + servletMapping + "/widgets/acme/123?updated-min=2007-10-09T22:42:26.000Z&locale=en",  TargetType.TYPE_ENTRY );

        checkRegex( "/" + servletMapping + "/widgets/acme/123/3",  TargetType.TYPE_ENTRY );
        checkRegex( "/" + servletMapping + "/widgets/acme/123/3?xyz=p1&abc=p2&mno=p3",  TargetType.TYPE_ENTRY );
        checkRegex( "/" + servletMapping + "/widgets/acme/123/3?updated-min=2007-10-09T22:42:26.000Z&locale=en",  TargetType.TYPE_ENTRY );
        checkRegex( "/" + servletMapping + "/widgets/acme/123.en.xml/3",  TargetType.TYPE_ENTRY );
        checkRegex( "/" + servletMapping + "/widgets/acme/123.en.xml/3?xyz=p1&abc=p2&mno=p3",  TargetType.TYPE_ENTRY );
        checkRegex( "/" + servletMapping + "/widgets/acme/123.en.xml/3?updated-min=2007-10-09T22:42:26.000Z&locale=en",  TargetType.TYPE_ENTRY );
    }

    private void checkRegex( String uri, TargetType expectedType ) {
        log.debug( "uri INPUT= " + uri );
        MockRequestContext requestContext = new MockRequestContext( serviceContext, "GET", uri ); 
        log.debug( "URI= " + requestContext.getTargetPath() );

        log.debug( "Calling targetResolver.resolve" );
        Target target = targetResolver.resolve( requestContext );
        assertNotNull( target );
        
        TargetType type = target.getType();
        log.debug( "TargetType = " + type );
        assertTrue( type.equals( expectedType ) );
    }
    
}
