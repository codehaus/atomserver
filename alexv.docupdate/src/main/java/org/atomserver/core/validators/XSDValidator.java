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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.parsers.DOMParser;
import org.atomserver.ContentValidator;
import org.atomserver.exceptions.BadContentException;
import org.xml.sax.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

/**
 * XSDValidator - ContentValidator implementation to validate XML contend against XSDs.
 *
 * TODO: this is still under development -- needs test cases and completion.
 * 
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class XSDValidator implements ContentValidator, ErrorHandler {

    private static final Log log = LogFactory.getLog(XSDValidator.class);

    private DOMParser parser;
    private boolean ignoreWarnings = true;

    public void validate(String content) throws BadContentException {
        try {
            parser.parse(new InputSource(new StringReader(content)));
        } catch (Exception e) {
            throw new BadContentException(e);
        }
    }

    public void setNamespaceMappings(Properties properties)
            throws SAXNotSupportedException, SAXNotRecognizedException, IOException {
        parser = new DOMParser();
        parser.setFeature("http://xml.org/sax/features/validation", true);
        Enumeration<?> uris = properties.propertyNames();
        while (uris.hasMoreElements()) {
            String uri = (String) uris.nextElement();
            String xsdLocation = properties.getProperty(uri);
            File tmpFile = File.createTempFile("temp", ".xsd");
            InputStream s = getClass().getClassLoader().getResourceAsStream(xsdLocation);
            if (s != null) {
                IOUtils.copy(s, new FileWriter(tmpFile));
                log.debug("found " + xsdLocation + " as a classpath resource");
            } else {
                File fileLoc = new File(xsdLocation);
                if (fileLoc.exists() && fileLoc.isFile()) {
                    FileUtils.copyFile(fileLoc, tmpFile);
                    log.debug("found " + xsdLocation + " as a file system resource");
                } else {
                    try {
                        URL urlLoc = new URL(xsdLocation);
                        FileUtils.copyURLToFile(urlLoc, tmpFile);
                        log.debug("found " + xsdLocation + " as a url resource");
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException("could not find XSD resource:" +
                                                           xsdLocation);
                    }
                }
            }


            parser.setProperty(uri, tmpFile.getAbsolutePath());
        }
        parser.setErrorHandler(this);
    }

    public void setIgnoreWarnings(boolean ignoreWarnings) {
        this.ignoreWarnings = ignoreWarnings;
    }

    public void warning(SAXParseException e) throws SAXException {
        if (!ignoreWarnings) {
            throw e;
        }
    }

    public void error(SAXParseException e) throws SAXException {
        throw e;
    }

    public void fatalError(SAXParseException e) throws SAXException {
        throw e;
    }
}
