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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.exceptions.BadContentException;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import java.io.Reader;
import java.io.StringReader;

import com.ctc.wstx.stax.WstxInputFactory; 

import org.atomserver.ContentValidator;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class SimpleXMLContentValidator implements ContentValidator {

    private static final Log log = LogFactory.getLog(SimpleXMLContentValidator.class);

    /**
     */
    public void validate( String contentXml ) throws BadContentException {
        validateNotEmpty( contentXml );
        validateWellFormed( new StringReader( contentXml ) );
    }

    private void validateNotEmpty( String contentXml ) throws BadContentException  {

        if ( contentXml == null || contentXml.equals( "" ) ) {
            if (log.isDebugEnabled()) 
                log.debug("*********************** contentXml= " + contentXml );
            
            String msg = "Entry <content> contains NULL or empty XML" ;
            log.error(msg);
            throw new BadContentException( msg );
        }
    }

    private void validateWellFormed( Reader reader ) throws BadContentException  {
        try { 
            // we will simply walk the doc and see if it throws an Exception
            XMLInputFactory xif = new WstxInputFactory();
            XMLStreamReader xmlreader = xif.createXMLStreamReader(reader );
            
            while ( xmlreader.hasNext() ) {
                // Errors won't occur unless we actually access the data...
                touchEvent( xmlreader );
            }
            xmlreader.close();
        } catch ( XMLStreamException ee ) {
            String msg = "Not well-formed XML :: XMLStreamException:: " + ee.getMessage() ;
            log.error(msg);
            throw new BadContentException( msg );
        }
    }

    private void touchEvent( XMLStreamReader reader ) throws XMLStreamException {
        int eventCode = reader.next();
        String val = null;
        switch (eventCode) {
        case XMLStreamReader.START_ELEMENT:
            val= reader.getLocalName(); 
            if ( log.isTraceEnabled() ) {
                log.trace("event = START_ELEMENT");
                log.trace("Localname = "+val);
            }
            break;
        case XMLStreamReader.END_ELEMENT :
            val= reader.getLocalName(); 
            if ( log.isTraceEnabled() ) {
                log.trace("event = END_ELEMENT");
                log.trace("Localname = "+val);
            }
            break;
        case XMLStreamReader.PROCESSING_INSTRUCTION :
            val= reader.getPIData();
            if ( log.isTraceEnabled() ) {
                log.trace("event = PROCESSING_INSTRUCTION");
                log.trace("PIData = " + val);
            }
            break;
        case XMLStreamReader.CHARACTERS :
            val= reader.getText();
            if ( log.isTraceEnabled() ) {
                log.trace("event = CHARACTERS");
                log.trace("Characters = " + val);
            }
            break;
        case XMLStreamReader.COMMENT :
            val= reader.getText();
            if ( log.isTraceEnabled() ) {
                log.trace("event = COMMENT");
                log.trace("Comment = " + val);
            }
            break;
        case XMLStreamReader.SPACE :
            val= reader.getText();
            if ( log.isTraceEnabled() ) {
                log.trace("event = SPACE");
                log.trace("Space = " + val);
            }
            break;
        case XMLStreamReader.START_DOCUMENT :
            if ( log.isTraceEnabled() ) {
                log.trace("event = START_DOCUMENT");
                log.trace("Document Started.");
            }
            break;
        case XMLStreamReader.END_DOCUMENT :
            if ( log.isTraceEnabled() ) {
                log.trace("event = END_DOCUMENT");
                log.trace("Document Ended");
            }
            break;
        case XMLStreamReader.ENTITY_REFERENCE :
            val= reader.getText();
            if ( log.isTraceEnabled() ) {
                log.trace("event = ENTITY_REFERENCE");
                log.trace("Text = " + val);
            }
            break;
        case XMLStreamReader.DTD :
            val= reader.getText();
            if ( log.isTraceEnabled() ) {
                log.trace("event = DTD");
                log.trace("DTD = " + val);
            }           
            break;
        case XMLStreamReader.CDATA :
            val= reader.getText();
            if ( log.isTraceEnabled() ) {
                log.trace("event = CDATA");
                log.trace("CDATA = " + val);
            }
            break;
        }
    }
    
}
