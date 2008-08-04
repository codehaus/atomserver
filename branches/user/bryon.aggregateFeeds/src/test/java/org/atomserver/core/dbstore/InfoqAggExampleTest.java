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

import org.atomserver.testutils.conf.TestConfUtil;
import org.atomserver.core.BaseServiceDescriptor;
import org.apache.abdera.model.Feed;

public class InfoqAggExampleTest  extends DBSTestCase {

    static String cberryXML = "<employee xmlns='http://schemas.atomserver.org/examples'\n" +
                              "          name='Chris Berry' id='123' dept='dev' />";
    static String bjacobXML = "<employee xmlns='http://schemas.atomserver.org/examples'\n" +
                              "          name='Bryon Jacob' id='345' dept='dev' />";
    static String meetingXML = "<meeting xmlns='http://schemas.atomserver.org/examples'\n" +
                               "         name='standup' time='Every Tuesday 9:15' >\n" +
                               "     <employee id='123' />\n" +
                               "     <employee id='345' />\n" +
                               "</meeting>";

    public void setUp() throws Exception {
        TestConfUtil.preSetup("infoq");
        super.setUp();

        entryCategoriesDAO.deleteAllEntryCategories("employees");
        entryCategoriesDAO.deleteAllEntryCategories("meetings");

        entriesDao.deleteAllEntries(new BaseServiceDescriptor("employees"));
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("meetings"));

        modifyEntry("employees", "acme", "cberry", null, cberryXML, true, "0");
        modifyEntry("employees", "acme", "bjacob", null, bjacobXML, true, "0");
        modifyEntry("meetings", "acme", "standup", null, meetingXML, true, "0");
    }

    public void tearDown() throws Exception {
        super.tearDown();
        TestConfUtil.postTearDown();
    }

    public void testAggregateFeeds() throws Exception {
        // first, check that the individual entry feeds are the size we expect:
        Feed feed = getPage("employees/acme");
        assertEquals(2, feed.getEntries().size());
        feed = getPage("meetings/acme");
        assertEquals(1, feed.getEntries().size());

        // get the aggregate feed
        feed = getPage("$join/urn:EID?entry-type=full");
        log.debug( "Feed = " + feed );

        java.io.StringWriter stringWriter = new java.io.StringWriter();
        feed.writeTo( abdera.getWriterFactory().getWriter("PrettyXML"), stringWriter );
        log.debug( "AGG FEED = \n" + stringWriter.toString() );

        assertEquals(2, feed.getEntries().size());
    }
}
