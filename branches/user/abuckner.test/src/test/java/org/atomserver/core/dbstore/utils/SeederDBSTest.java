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


package org.atomserver.core.dbstore.utils;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.core.BaseFeedDescriptor;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.dbstore.dao.EntriesDAO;
import org.atomserver.utils.conf.ConfigurationAwareClassLoader;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Date;
import java.util.List;

public class SeederDBSTest extends TestCase {

    static private Log log = LogFactory.getLog(SeederDBSTest.class);
    static protected final Date ZERO_DATE = new Date(0L);

    static private boolean hasRun = false;

    static public boolean hasRun() { return hasRun; }

    // -------------------------------------------------------
    public static Test suite() { return new TestSuite(SeederDBSTest.class); }

    // -------------------------------------------------------
    protected void setUp() throws Exception { super.setUp(); }

    // -------------------------------------------------------
    protected void tearDown() throws Exception { super.tearDown(); }

    public SeederDBSTest() {}

    public SeederDBSTest(String name) { super(name); }

    //----------------------------
    //          Tests
    //----------------------------
    public void testSeedDB() throws Exception {

        if (hasRun) {
            return;
        }

        String[] configs = {
                "/org/atomserver/spring/propertyConfigurerBeans.xml",
                "/org/atomserver/spring/logBeans.xml",
                "/org/atomserver/spring/storageBeans.xml",
                "/org/atomserver/spring/databaseBeans.xml"
        };
        ClassPathXmlApplicationContext springFactory =
                new ClassPathXmlApplicationContext(configs, false);
        springFactory.setClassLoader(
                new ConfigurationAwareClassLoader(springFactory.getClassLoader()));
        springFactory.refresh();


        try {
            DBSeeder.getInstance(springFactory).seedEntriesClearingFirst();
        } catch ( Exception ee ) {
            ee.printStackTrace();
        }
        hasRun = true;

        // We should now get an ordered List back
        // SORTED -- From the beginning of time

        EntriesDAO widgetsDAOiBatis = (EntriesDAO) springFactory.getBean("org.atomserver-entriesDAO");

        List sortedList =
                widgetsDAOiBatis.selectEntriesByLastModifiedSeqNum(new BaseFeedDescriptor("widgets", null), ZERO_DATE);
        log.debug("List= " + sortedList);

        Date lastVal = ZERO_DATE;
        long seqNum = 0;
        for (Object obj : sortedList) {
            EntryMetaData widget = (EntryMetaData) obj;

            assertTrue(lastVal.compareTo(widget.getUpdatedDate()) <= 0);
            lastVal = widget.getUpdatedDate();

            assertTrue( "expected " + seqNum + " < " + widget.getUpdateTimestamp() + " for " + widget,
                        seqNum < widget.getUpdateTimestamp());

            seqNum = widget.getUpdateTimestamp();
        }
    }
}
