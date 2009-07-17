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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;
import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;

import java.util.*;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
abstract public class AbstractDAOiBatisImpl
        extends SqlMapClientDaoSupport
        implements AtomServerDAO {

    protected final Log log = LogFactory.getLog(AbstractDAOiBatisImpl.class);

    //============================================
    //    FOR TESTING ONLY 
    static private boolean testingForceFailure = false;

    static public void setTestingForceFailure( boolean tORf ) {
        testingForceFailure = tORf;
    }
 
    //======================================
    //   set the database type from Spring
    //======================================
    static public final String DEFAULT_DB_TYPE = "sqlserver";

    /**
     * valid values are "hsql", "mysql" and "sqlserver"
     */
    protected String dbType = DEFAULT_DB_TYPE;

    public String getDatabaseType() { return dbType; }

    public void setDatabaseType(String dbType) {
        if (DatabaseType.isValidType(dbType)) {
            log.info("Database Type = " + dbType);
            this.dbType = dbType;
        } else {
            throw new IllegalArgumentException(dbType + " is not a valid DatabaseType value");
        }
    }

    //======================================
    //          COUNT QUERIES
    //======================================
    public int getTotalCountInternal(String workspace, String collection, String iBatisSQLId ) {
        StopWatch stopWatch = new Log4JStopWatch();
        try {
            HashMap paramMap = new HashMap();
            paramMap.put("workspace", workspace);
            if (collection != null) {
                paramMap.put("collection", collection);
            }
            Integer count = (Integer) (getSqlMapClientTemplate().queryForObject( iBatisSQLId, paramMap));
            return ((count == null) ? 0 : count);
        }
        finally {
            stopWatch.stop("DB.getTotalCount", "");
        }
    }

    //======================================
    //          DELETE ALL ROWS
    //======================================
    public void deleteAllEntriesInternal(String workspace, String collection, String iBatisSQLId ) {
        HashMap paramMap = new HashMap();
        paramMap.put( "workspace", workspace );
        if (collection != null) {
            paramMap.put( "collection", collection );
        }

        final int deletedRows = getSqlMapClientTemplate().delete(iBatisSQLId, paramMap);
        log.debug("DELETED " + deletedRows + " rows from " + workspace + "/" + collection);
    }

    //======================================
    //        GET THE SERVER'S DATE
    //======================================
    public Date selectSysDate() {
        if ( testingForceFailure ) {
            throw new RuntimeException( "THIS IS A FAKE FAILURE FROM AbstractDAOiBatisImpl.testingForceFailure" );
        }

        return (Date) (getSqlMapClientTemplate().queryForObject("selectSysDate",
                                                                Collections.singletonMap("dbType", getDatabaseType())));
    }

    protected ParamMap paramMap() {
        return new ParamMap().param("dbType", getDatabaseType());
    }

    protected static class ParamMap extends HashMap<String, Object> {
        public ParamMap param(String name, Object value) {
            if (value != null) {
                put(name, value);
            }
            return this;
        }

        public ParamMap addLocaleInfo(Locale locale) {
            put("language", locale == null ? "**" : locale.getLanguage());

            String country = (locale == null || locale.getCountry().equals("")) ? "**" : locale.getCountry();
            put("country", country);

            return this;
        }

    }

    //======================================
    //        TEST AVAILABILTY
    //======================================
    /**
     * {@inheritDoc}
     */
    public void testAvailability() {
        Date dbDate = selectSysDate();
        if (log.isTraceEnabled())
            log.trace("dbDate= " + dbDate);
    }

}
