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

package org.atomserver.utils.acegi;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.acegisecurity.ConfigAttributeDefinition;
import org.acegisecurity.SecurityConfig;
import org.atomserver.utils.acegi.RESTfulDefinitionSource;


public class RESTfulDefinitionSourceTest extends TestCase {
       
    static private Log log = LogFactory.getLog(RESTfulDefinitionSourceTest.class);

    // -------------------------------------------------------
    public static Test suite() 
    { return new TestSuite( RESTfulDefinitionSourceTest.class ); }
    
    // -------------------------------------------------------
    protected void setUp() throws Exception 
    { super.setUp(); }
    
    // -------------------------------------------------------
    protected void tearDown() throws Exception
    { super.tearDown(); }
    
    //----------------------------
    //          Tests 
    //----------------------------
    public void testBasics() throws Exception {
        String url = "/v1/widgets/acme/2797.en.xml";
        String pathToRoleList = "/**:GET=ROLE_READER\n" + "/**:PUT,DELETE,POST=ROLE_WRITER\n" ;

        RESTfulDefinitionSource rds = new RESTfulDefinitionSource( pathToRoleList );

        log.debug( "========================" );
        ConfigAttributeDefinition cad = rds.lookupAttributes( url, "GET" );      
        log.debug( "ConfigAttributeDefinition cad= " + cad );
        SecurityConfig sc = new SecurityConfig( "ROLE_READER" );
        assertTrue( cad.contains( sc ) );

        log.debug( "========================" );
        cad = rds.lookupAttributes( url, "DELETE" );      
        log.debug( "ConfigAttributeDefinition cad= " + cad );
        sc = new SecurityConfig( "ROLE_WRITER" );
        assertTrue( cad.contains( sc ) );

        // should match the first for all methods
        log.debug( "========================" );
        String pathToRoleList2 = "/v1/**=ROLE_READER\n" + "/v1/**:PUT,DELETE,POST=ROLE_WRITER\n" ;
        rds = new RESTfulDefinitionSource( pathToRoleList2 );
        cad = rds.lookupAttributes( url, "DELETE" );      
        log.debug( "ConfigAttributeDefinition cad= " + cad );
        sc = new SecurityConfig( "ROLE_READER" );
        assertTrue( cad.contains( sc ) );

    }

    public void testFail()  throws Exception {
        log.debug( "========================" );
        String url = "/v1/widgets/acme/2797.en.xml";
        String pathToRoleList = "/**:GOO=ROLE_READER\n" + "/**:PUT,DELETE,POST=ROLE_WRITER\n" ;

        try { 
            RESTfulDefinitionSource rds = new RESTfulDefinitionSource( pathToRoleList );
            fail( "shouldnt get here" );
        } catch ( IllegalArgumentException ee ) { /* expected */ }
    }

    public void testNoMatch()  throws Exception {
        log.debug( "========================" );
        String url = "/v1/widgets/acme/2797.en.xml";
        String pathToRoleList = "foo/**:GET=ROLE_READER\n" + "foo/**:PUT,DELETE,POST=ROLE_WRITER\n" ;
        RESTfulDefinitionSource rds = new RESTfulDefinitionSource( pathToRoleList );
        ConfigAttributeDefinition cad = rds.lookupAttributes( url, "DELETE" );      
        log.debug( "ConfigAttributeDefinition cad= " + cad );
        assertNull( cad );
    }

}