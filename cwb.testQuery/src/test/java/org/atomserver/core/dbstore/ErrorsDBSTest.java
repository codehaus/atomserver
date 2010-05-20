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


package org.atomserver.core.dbstore;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.model.Entry;

import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.abdera.protocol.client.AbderaClient;
import org.atomserver.core.ErrorsAtomServerTestCase;
import org.atomserver.core.AtomServerTestCase;

/**
 */
public class ErrorsDBSTest extends ErrorsAtomServerTestCase {

    public static Test suite() { return new TestSuite(ErrorsDBSTest.class); }

    public void setUp() throws Exception { super.setUp(); }

    public void tearDown() throws Exception { super.tearDown(); }

    protected String getStoreName() { return "org.atomserver-atomService"; }

    public void testErrorHandling() throws Exception {
        runErrorHandlingTests();
    }

    public void testBadUpdate() throws Exception {
        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        Entry entry = AtomServerTestCase.getFactory().newEntry();
        entry.setId("widgets/acme/foo.bar.xml");
        entry.setContent("whatever");

        ClientResponse response = client.put( getServerURL() + "widgets/acme/foo.bar.xml", entry, options);
        junit.framework.Assert.assertEquals(400, response.getStatus());
        response.release();
    }

    public void testBadDelete() throws Exception {
        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        Entry entry = AtomServerTestCase.getFactory().newEntry();
        entry.setId("widgets/acme/foo.bar.xml");
        entry.setContent("whatever");

        ClientResponse response = client.delete( getServerURL() + "widgets/acme/foo.bar.xml", options);
        junit.framework.Assert.assertEquals(400, response.getStatus());
        response.release();
    }
}
