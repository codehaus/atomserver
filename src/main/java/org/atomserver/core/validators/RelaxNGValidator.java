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

import org.atomserver.exceptions.BadContentException;
import org.atomserver.ContentValidator;
import com.thaiopensource.util.SinglePropertyMap;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.ValidationDriver;
import com.thaiopensource.validate.rng.CompactSchemaReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.StringReader;

/**
 * RelaxNGValidator - implementation of the ContentValidator interface that validates the content
 * (must be XML) against a RelaxNG schema.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class RelaxNGValidator implements ContentValidator {

    private static final Log log = LogFactory.getLog(RelaxNGValidator.class);
    private static final int CHARS_TO_OUTPUT = 100;

    private ThreadLocal<ValidationDriver> validationDriver;

    public void validate(String contentXML) throws BadContentException {
        // null content is inherently invalid
        if (contentXML == null) {
            throw new BadContentException("Invalid Content: content is null");
        }

        // otherwise, try to validate the content against the validation driver, and throw a
        // BadContentException if it fails
        try {
            validationDriver.get().validate(new InputSource(new StringReader(contentXML)));
        } catch (Exception e) {
            int numChars = (contentXML.length() < CHARS_TO_OUTPUT) ? contentXML.length() : CHARS_TO_OUTPUT;
            String msg = "Document invalid - " + e.getClass().getName() + " message= " + e.getMessage() +
                         "\n The first 100 chars of the document:: " +
                         contentXML.substring(0, numChars);
            throw new BadContentException(msg, e);
        }
    }

    public void setSchemaLocation(final Resource schemaLocation) {
        validationDriver = new ThreadLocal<ValidationDriver>() {

            protected ValidationDriver initialValue() {
                ValidationDriver validationDriver =
                        new ValidationDriver(ERROR_HANDLER_PROPERTY_MAP,
                                             CompactSchemaReader.getInstance());
                try {
                    validationDriver.loadSchema(new InputSource(schemaLocation.getInputStream()));
                    return validationDriver;
                } catch (Exception e) {
                    log.error("exception loading schema", e);
                    return null;
                }
            }
        };
    }

    private static final SinglePropertyMap ERROR_HANDLER_PROPERTY_MAP =
            new SinglePropertyMap(
                    ValidateProperty.ERROR_HANDLER,
                    new ErrorHandler() {

                        public void warning(SAXParseException exception) throws SAXException {
                            throw exception;
                        }

                        public void error(SAXParseException exception) throws SAXException {
                            throw exception;
                        }

                        public void fatalError(SAXParseException exception) throws SAXException {
                            throw exception;
                        }
                    });

}
