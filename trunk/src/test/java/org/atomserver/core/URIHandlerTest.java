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


package org.atomserver.core;

import junit.framework.TestCase;
import org.apache.abdera.i18n.iri.IRI;

public class URIHandlerTest extends TestCase {

    public void testIRIParsing() throws Exception {
        IRI baseIri = new IRI("http://whatever.com/a/b");
        IRI iri = new IRI("http://whatever.com/a/b/c?easy-as=123&simple-as=do,re,mi");
        System.out.println("iri = " + iri);
        System.out.println("iri.getPath() = " + iri.getPath());
        System.out.println("iri.getQuery() = " + iri.getQuery());
        System.out.println("iri.getRawQuery() = " + iri.getRawQuery());
        System.out.println("-----");
        iri = baseIri.relativize(iri);
        System.out.println("iri = " + iri);
        System.out.println("iri.getPath() = " + iri.getPath());
        System.out.println("iri.getQuery() = " + iri.getQuery());
        System.out.println("iri.getRawQuery() = " + iri.getRawQuery());
    }

}
