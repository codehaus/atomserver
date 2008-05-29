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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.atomserver.testutils.client.JettyWebAppTestCase;
import org.atomserver.testutils.conf.TestConfUtil;

import java.io.File;

public class ExtendedContextTest extends JettyWebAppTestCase {
    private String prevConfDir = null;

    protected void setUp() throws Exception {
        TestConfUtil.preSetup("confdir");
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestConfUtil.postTearDown();
    }


    public void testExtendedContext() throws Exception {
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod("http://localhost:" + getPort() + "/atomserver/v1");
        client.executeMethod(get);

        assertEquals("<?xml version='1.0' encoding='UTF-8'?>" +
                     "<service xmlns=\"http://www.w3.org/2007/app\" " +
                     "xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
                     "<workspace>" +
                     "<atom:title type=\"text\">exttest</atom:title>" +
                     "</workspace>" +
                     "</service>",
                     get.getResponseBodyAsString());

        assertEquals("ext lib works", getSpringFactory().getBean("foobarbean").toString());
    }
}
