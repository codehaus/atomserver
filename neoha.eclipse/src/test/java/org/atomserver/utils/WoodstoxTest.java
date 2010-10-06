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

package org.atomserver.utils;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.stax.WstxInputFactory;

/**
 */
public class WoodstoxTest extends TestCase {

    public static Test suite() 
    { return new TestSuite( WoodstoxTest.class ); }

    public void tearDown() throws Exception 
    { super.tearDown(); } 

    public void setUp() throws Exception 
    { super.tearDown(); } 

    public void testWoodstox1() throws Exception {
        // we will simply walk the doc and see if it throws an Exception
        XMLInputFactory xif = new WstxInputFactory();
        XMLStreamReader r = xif.createXMLStreamReader( getClass().getClassLoader().getResourceAsStream("testwidget.xml") );
        while (r.hasNext()) r.next();
        r.close();
    }

    // FIXME:: deal w/ attributes
    public void testWoodstox2() throws Exception {
        // we will simply walk the doc and see if it throws an Exception
        XMLInputFactory xif = new WstxInputFactory();
        XMLStreamReader reader = xif.createXMLStreamReader( getClass().getClassLoader().getResourceAsStream("testwidget.xml") );

        while ( reader.hasNext() ) {
            printEventInfo( reader );
        }
        reader.close();
    }

    private static void printEventInfo(XMLStreamReader reader) throws XMLStreamException {
        int eventCode = reader.next();
        switch (eventCode) {
            case 1 :
			reader.getLocalName(); 
                //log.debug("event = START_ELEMENT");
                //log.debug("Localname = "+val);
                break;
            case 2 :
			reader.getLocalName(); 
                //log.debug("event = END_ELEMENT");
                //log.debug("Localname = "+val);
                break;
            case 3 :
			reader.getPIData();
                //log.debug("event = PROCESSING_INSTRUCTION");
                //log.debug("PIData = " + val);
                break;
            case 4 :
			reader.getText();
                //log.debug("event = CHARACTERS");
                //log.debug("Characters = " + val);
                break;
            case 5 :
			reader.getText();
                //log.debug("event = COMMENT");
                //log.debug("Comment = " + val);
                break;
            case 6 :
			reader.getText();
                //log.debug("event = SPACE");
                //log.debug("Space = " + val);
                break;
            case 7 :
                //log.debug("event = START_DOCUMENT");
                //log.debug("Document Started.");
                break;
            case 8 :
                //log.debug("event = END_DOCUMENT");
                //log.debug("Document Ended");
                break;
            case 9 :
			reader.getText();
                //log.debug("event = ENTITY_REFERENCE");
                //log.debug("Text = " + val);
                break;
            case 11 :
			reader.getText();

                break;
            case 12 :
			reader.getText();
                //log.debug("event = CDATA");
                //log.debug("CDATA = " + val);
                break;
        }
    }

}
