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

import org.atomserver.exceptions.OptimisticConcurrencyException;


public class OptimisticConcurrencyExceptionTest extends TestCase {

    public static Test suite() 
    { return new TestSuite( OptimisticConcurrencyExceptionTest.class ); }
    
    protected void setUp() throws Exception 
    { super.setUp(); }
    
    protected void tearDown() throws Exception
    { super.tearDown(); }
    
    //----------------------------
    //          Tests 
    //----------------------------
    public void testException() throws Exception {

        String editURI = "what/ever";
        
        /* 
        OptimisticConcurrencyException ee = new OptimisticConcurrencyException(editURI);
        assertNull( ee.getMessage() );
        assertEquals( editURI, ee.getEditURI() );
        */

        OptimisticConcurrencyException ee = new OptimisticConcurrencyException( "whatever", editURI );
        assertEquals( "whatever", ee.getMessage() );
        assertEquals( editURI, ee.getEditURI() );

        ee = new OptimisticConcurrencyException( new NullPointerException(), editURI );
        assertTrue( ee.getCause() instanceof NullPointerException );
        assertEquals( editURI, ee.getEditURI() );

        ee = new OptimisticConcurrencyException( "whatever", new NullPointerException(), editURI );
        assertEquals( "whatever", ee.getMessage() );
        assertTrue( ee.getCause() instanceof NullPointerException );
        assertEquals( editURI, ee.getEditURI() );
    }
}
