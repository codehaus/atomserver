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

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.commons.io.FileUtils;
import org.atomserver.core.BaseServiceDescriptor;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Show that we can store non-localized entries into atomserver
 */
public class NonLocalizedEntriesDBSTest extends DBSTestCase {


    public void setUp() throws Exception {
        super.setUp();
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("dummy"));
    }

    public void testNonLocalized() throws Exception {
        for (int propId = 40000; propId < 40004; propId++) {
            createWidget("dummy", "acme", String.valueOf(propId), null,
                         createWidgetXMLFileString(String.valueOf(propId)));
        }

        Feed feed = getPage( "dummy/acme" );
        assertEquals( 4, feed.getEntries().size() ); 

        for (Entry entry : feed.getEntries()) {
            String url = entry.getId().toString();
            log.debug("url = " + url);

            Matcher dummyIdMatcher = Pattern.compile(".*dummy/acme/(\\d+)/?.*").matcher(url);
            assertTrue(dummyIdMatcher.matches());
            String dummyId = dummyIdMatcher.group(1);
            log.debug( "dummyId = " + dummyId);
            int id = Integer.parseInt( dummyId );
            assertTrue( id >= 40000 && id < 40004 );
        }

        for (int propId = 40000; propId < 40004; propId++) {
            deleteEntry("dummy", "acme", String.valueOf(propId), null);
        }
        FileUtils.deleteDirectory(new File("var/dummy/acme"));
    }
}