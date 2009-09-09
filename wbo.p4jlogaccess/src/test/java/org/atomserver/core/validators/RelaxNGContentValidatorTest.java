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
import org.atomserver.exceptions.BadContentException;
import org.springframework.core.io.ClassPathResource;

public class RelaxNGContentValidatorTest extends TestCase {

    private static final Log log = LogFactory.getLog(RelaxNGContentValidatorTest.class);

    public static Test suite() { return new TestSuite(RelaxNGContentValidatorTest.class); }

    public void testSimpleSchema() throws Exception {
        RelaxNGValidator validator = new RelaxNGValidator();
        validator.setSchemaLocation(new ClassPathResource("test.rnc"));

        checkValidity(validator, "<foo xmlns='http://atomserver.org/test' id='1'><bar>hi</bar></foo>", false);
        checkValidity(validator, "<foo xmlns='http://atomserver.org/test' id='1'></foo>", true);
    }

    public void testWidgetSchema() throws Exception {
        RelaxNGValidator validator = new RelaxNGValidator();
        ClassPathResource schemaLocation =
                    new ClassPathResource("widgets-1.0.rnc");
        validator.setSchemaLocation(schemaLocation);
        checkValidity(validator, VALID_WIDGET_XML, false);
        checkValidity(validator, INVALID_WIDGET_XML, true);
    }

    private void checkValidity(RelaxNGValidator validator, String xml, boolean expectException) {
        try {
            validator.validate(xml);
            assertFalse("we expected the xml to be invalid", expectException);
        } catch (BadContentException e) {
            if (!expectException) { throw e; }
        }
    }

    private static final String VALID_WIDGET_XML =
            "<property xmlns=\"http://schemas.atomserver.org/widgets/v1/rev0\" systemId=\"acme\" id=\"1234\" inNetwork=\"false\">\n"
               + "<colors>"
               + "<color isDefault=\"true\">teal</color>"
               + "</colors>"
               + "<contact>"
               + "<contactId>1638</contactId>"
               + "<displayName>This is an insert</displayName>"
               + "<hasEmail>true</hasEmail>"
               + "</contact>"
               + "</property>";

    private static final String INVALID_WIDGET_XML =
            "<property xmlns=\"http://schemas.atomserver.org/widgets/v1/rev0\" systemId=\"acme\" id=\"1234\" inNetwork=\"false\">\n"
               + "<colors>"
               + "<foooooooo isDefault=\"true\">teal</foooooooo>"
               + "</colors>"
               + "<contact>"
               + "<contactId>1638</contactId>"
               + "<displayName>This is an insert</displayName>"
               + "<hasEmail>true</hasEmail>"
               + "</contact>"
               + "</property>";
}
