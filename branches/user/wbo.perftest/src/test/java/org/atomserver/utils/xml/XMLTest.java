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

package org.atomserver.utils.xml;

import junit.framework.TestCase;

public class XMLTest extends TestCase {
    public void testXMLGenerator() throws Exception {
        XML xml = XML.element("entry", "http://schemas.atomserver.org/atomserver/v1/rev0")
                .attr("application", "FOO")
                .attr("type", "LOGIN")
                .attr("brand", "foobar")
                .attr("time", "2008-03-04T09:30:45")
                .add(XML.element("message").addCdata("My Message"))
                .add(XML.element("header").attr("name", "FOO").attr("value", "BAR"))
                .add(XML.element("header").attr("name", "FOO2").attr("value", "BAR2"))
                .add(XML.element("browser").attr("name", "FOO").attr("value", "BAR"))
                .add(XML.element("app").attr("name", "FOO").attr("value", "BAR"));

        assertEquals("<entry xmlns='http://schemas.atomserver.org/atomserver/v1/rev0' " +
                     "application='FOO' type='LOGIN' brand='foobar' " +
                     "time='2008-03-04T09:30:45'>" +
                     "<message><![CDATA[My Message]]></message>" +
                     "<header name='FOO' value='BAR'/>" +
                     "<header name='FOO2' value='BAR2'/>" +
                     "<browser name='FOO' value='BAR'/>" +
                     "<app name='FOO' value='BAR'/>" +
                     "</entry>",
                     xml.toString());
    }
}
