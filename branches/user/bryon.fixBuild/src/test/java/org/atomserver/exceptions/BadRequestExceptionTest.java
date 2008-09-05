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

package org.atomserver.exceptions;

import junit.framework.Test; 
import junit.framework.TestCase; 
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.exceptions.BadRequestException;


public class BadRequestExceptionTest extends TestCase {

    static private Log log = LogFactory.getLog( BadRequestExceptionTest.class );

    public static Test suite() 
    { return new TestSuite( BadRequestExceptionTest.class ); }
    
    protected void setUp() throws Exception 
    { super.setUp(); }
    
    protected void tearDown() throws Exception
    { super.tearDown(); }
    
    //----------------------------
    //          Tests 
    //----------------------------
    public void testException() throws Exception {
        
        BadRequestException ee = new BadRequestException();
        assertNull( ee.getMessage() );

        ee = new BadRequestException( "whatever" );
        assertEquals( "whatever", ee.getMessage() );

        ee = new BadRequestException( new NullPointerException() );
        assertTrue( ee.getCause() instanceof NullPointerException );

        ee = new BadRequestException( "whatever", new NullPointerException() );
        assertEquals( "whatever", ee.getMessage() );
        assertTrue( ee.getCause() instanceof NullPointerException );
    }
}
