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

package org.atomserver.utils.collections;

import java.util.Map;
import java.util.HashMap;

import org.atomserver.utils.collections.BidirectionalMap;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class BidirectionalMapTest extends TestCase {

    // -------------------------------------------------------
    public static Test suite() 
    { return new TestSuite( BidirectionalMapTest.class ); }
    
    // -------------------------------------------------------
    protected void setUp() throws Exception 
    { super.setUp(); }
    
    // -------------------------------------------------------
    protected void tearDown() throws Exception
    { super.tearDown(); }
    
    //----------------------------
    //          Tests 
    //----------------------------
    public void testReverseGet() {
        // No need to testutils all the other methods that are part of HashMap

        Map regularMap = new HashMap<String,String>();
        regularMap.put( "dog", "pluto" );
        regularMap.put( "cat", "sylvester" );
        regularMap.put( "mouse", "micky" );

        BidirectionalMap<String,String> bdmap = new BidirectionalMap<String,String>( regularMap );

        assertEquals( "dog", bdmap.reverseGet( "pluto" ) ); 
        assertEquals( "cat", bdmap.reverseGet( "sylvester" ) ); 
        assertEquals( "mouse", bdmap.reverseGet( "micky" ) ); 
    }
}
