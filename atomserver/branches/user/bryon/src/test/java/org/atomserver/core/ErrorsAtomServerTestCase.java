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

import org.apache.abdera.protocol.client.ClientResponse;
import org.atomserver.core.dbstore.DBSTestCase;

/**
 */
abstract public class ErrorsAtomServerTestCase extends DBSTestCase {

    public void setUp() throws Exception
    { super.setUp(); }

    public void tearDown() throws Exception
    { super.tearDown(); }

    protected void runErrorHandlingTests() throws Exception {
        badURI();
        readService1();
        readFeedError1();
        readFeedError2();
        getEntryNoFile1();
        getEntryNoFile2();
        getEntryNoFile3();
        getEntryNoFile4();
        getEntryNoFile5();
    }

    public void badURI() throws Exception {
        // FIXME: this should be 400 (bad request)
        ClientResponse response= clientGet( "a/b/c/d/e", null, 404 );
        response.release();
    }

    public void readService1() throws Exception {
        ClientResponse response= clientGet( "UNKNOWN", null, 400 );
        response.release();
    }

    public void readFeedError1() throws Exception {
        ClientResponse response= clientGet( "UNKNOWN/acme", null, 400 );
        response.release();
    }

    public void readFeedError2() throws Exception {
        ClientResponse response= clientGet( "widgets/UNKNOWN", null, 400 );
        response.release();
    }

    public void getEntryNoFile1() throws Exception {
        ClientResponse response= clientGet( "widgets/acme/UNKNOWN.en.xml", null, 404 );
        response.release();
    }

    // this fails because a locale is expected for widgets
    public void getEntryNoFile2() throws Exception {
        ClientResponse response= clientGet( "widgets/acme/UNKNOWN.xml", null, 400 );
        response.release();
    }

    public void getEntryNoFile3() throws Exception {
        ClientResponse response= clientGet( "widgets/UNKNOWN/4.en.xml", null, 400 );
        response.release();
    }

    // Should be NOT FOUND 404
    public void getEntryNoFile4() throws Exception {
        ClientResponse response= clientGet( "widgets/acme/666.en.xml", null, 404 );
        response.release();
    }

    public void getEntryNoFile5() throws Exception {
        ClientResponse response= clientGet( "UNKNOWN/acme/9999.en.xml", null, 400 );
        response.release();
    }
}
