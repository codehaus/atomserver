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

package org.atomserver.core.dbstore.dao;

import junit.framework.TestCase;
import org.apache.abdera.Abdera;
import org.apache.abdera.protocol.server.ServiceContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.AtomService;
import org.atomserver.uri.URIHandler;
import org.atomserver.utils.conf.ConfigurationAwareClassLoader;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Date;


public class DAOTestCase extends TestCase {

    protected static final int DB_CATCHUP_SLEEP = 300; 

    protected Log log = LogFactory.getLog(DAOTestCase.class);
    protected static final String workspace = "widgets";

    static protected String userDir = System.getProperty("user.dir");

    protected ClassPathXmlApplicationContext springFactory = null;

    protected EntriesDAOiBatisImpl entriesDAO = null;
    protected EntryCategoriesDAO entryCategoriesDAO = null;
    protected ContentDAO contentDAO = null;
    protected EntryCategoryLogEventDAO entryCategoryLogEventDAO = null;
    protected StatisticsMonitorDAO statisticsMonitorDAO = null;

    protected URIHandler entryURIHelper;

    protected ServiceContext serviceContext = null;
    static private final String CONTEXT_NAME = "org.apache.abdera.protocol.server.ServiceContext";

    // -------------------------------------------------------
    protected void setUp() throws Exception {
        super.setUp();
        String[] configs = {"/org/atomserver/spring/propertyConfigurerBeans.xml",
                            "/org/atomserver/spring/databaseBeans.xml",
                            "/org/atomserver/spring/storageBeans.xml",
                            "/org/atomserver/spring/logBeans.xml",
                            "/org/atomserver/spring/abderaBeans.xml"};

        springFactory = new ClassPathXmlApplicationContext(configs, false);
        springFactory.setClassLoader( new ConfigurationAwareClassLoader(springFactory.getClassLoader()));
        springFactory.refresh();

        entryURIHelper = ((AtomService) springFactory.getBean("org.atomserver-atomService")).getURIHandler();

        entriesDAO = (EntriesDAOiBatisImpl) springFactory.getBean("org.atomserver-entriesDAO");
        entryCategoriesDAO = (EntryCategoriesDAO) springFactory.getBean("org.atomserver-entryCategoriesDAO");
        contentDAO = (ContentDAO) springFactory.getBean("org.atomserver-contentDAO");
        entryCategoryLogEventDAO = (EntryCategoryLogEventDAO) springFactory.getBean("org.atomserver-entryCategoryLogEventDAO");
        statisticsMonitorDAO =  (StatisticsMonitorDAO) springFactory.getBean("org.atomserver-statsMonitorDAO");

        serviceContext = (ServiceContext) springFactory.getBean(CONTEXT_NAME);
        if (serviceContext.getAbdera() == null) {
            serviceContext.init(new Abdera(), null );
        }
    }

    // -------------------------------------------------------
    protected void tearDown() throws Exception { super.tearDown(); }

    // -------------------------------------------------------
    static protected final Date ZERO_DATE = new Date(0L);

    static protected final long MYSQL_PRECISION = 999;

    protected boolean datesAreEqual(Date date1, Date date2) {
        return (compareDates(date1, date2) == 0);
    }

    protected int compareDates(Date date1, Date date2) {
        long ldate1 = date1.getTime();
        long ldate2 = date2.getTime();

        log.debug("COMPARING " + ldate1 + " to " + ldate2);

        long leftover = Math.abs(ldate1 - ldate2);
        if (leftover <= MYSQL_PRECISION) {
            return 0;
        } else if (leftover < 0) {
            return -1;
        } else {
            return 1;
        }
    }

}