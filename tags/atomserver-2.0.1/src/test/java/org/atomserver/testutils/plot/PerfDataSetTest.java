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

package org.atomserver.testutils.plot;

import java.net.URL;
import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.atomserver.testutils.plot.PerfCSVReader;
import org.atomserver.testutils.plot.PerfDataSet;


public class PerfDataSetTest extends TestCase {

    static private Log log = LogFactory.getLog(PerfDataSetTest.class );
    
    // -------------------------------------------------------
    public static Test suite() 
    { return new TestSuite( PerfDataSetTest.class ); }
    
    // -------------------------------------------------------
    protected void setUp() throws Exception 
    { super.setUp(); }
    
    // -------------------------------------------------------
    protected void tearDown() throws Exception
    { super.tearDown(); }
    
    //----------------------------
    //          Tests 
    //----------------------------
    public void test1() throws Exception {
        URL url = this.getClass().getResource( "/test.csv" );
        File csvFile = new File( url.toURI() ); 

        PerfCSVReader csvReader = new PerfCSVReader();
        PerfDataSet dataSet = csvReader.readCSVFile( csvFile );

        assertNotNull( dataSet );

        // FIXME asserts 
    }
}
