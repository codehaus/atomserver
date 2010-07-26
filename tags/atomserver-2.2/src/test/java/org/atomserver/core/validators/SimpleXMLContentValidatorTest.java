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

package org.atomserver.core.validators;

import junit.framework.Test; 
import junit.framework.TestCase; 
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.atomserver.core.validators.SimpleXMLContentValidator;
import org.atomserver.exceptions.BadContentException;


public class SimpleXMLContentValidatorTest extends TestCase {

    static private Log log = LogFactory.getLog( SimpleXMLContentValidatorTest.class );

    static private String badXML = "<foo>blah</bar>";
    static private String goodXML = "<foo>blah</foo>";

    public static Test suite() 
    { return new TestSuite( SimpleXMLContentValidatorTest.class ); }
    
    protected void setUp() throws Exception 
    { super.setUp(); }
    
    protected void tearDown() throws Exception
    { super.tearDown(); }
    
    //----------------------------
    //          Tests 
    //----------------------------
    public void testValidate() throws Exception {       
        runValidateTest( null, true );
        runValidateTest( "", true  );
        runValidateTest( badXML, true  );
        runValidateTest( goodXML, false  );
    }
    
    private void runValidateTest( String xml, boolean expectFail ) throws Exception {
        SimpleXMLContentValidator validator = new SimpleXMLContentValidator();
        try {
            validator.validate( xml );
            if ( expectFail ) 
                fail( "should not get here" );
        } catch (  BadContentException ee ) {
            if ( expectFail ) 
                return;
            else 
                fail( "should not fail" );    
        }     
    } 
}
