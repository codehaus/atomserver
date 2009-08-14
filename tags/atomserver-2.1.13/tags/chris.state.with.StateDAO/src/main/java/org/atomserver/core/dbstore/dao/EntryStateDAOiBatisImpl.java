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

import org.atomserver.core.EntryState;
import org.atomserver.utils.perf.AutomaticStopWatch;
import org.atomserver.utils.perf.StopWatch;
import org.atomserver.EntryDescriptor;

import java.util.Date;


/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class EntryStateDAOiBatisImpl
        extends AbstractDAOiBatisImpl
        implements EntryStateDAO {

    //======================================
    //   CRUD methods for entries table
    //======================================
    //-----------------------
    //       INSERT
    //-----------------------
    public Object insertEntryState(EntryState state) {
        StopWatch stopWatch = new AutomaticStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryStateDAOiBatisImpl INSERT ==> " + state);
        }

        try {
            return getSqlMapClientTemplate().insert("insertEntryState-" + getDatabaseType(), state);
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.insertEntryState", "", stopWatch);
            }
        }
    }

    //-----------------------
    //       SELECT
    //-----------------------
    /**
     */
    public EntryState selectEntryState(EntryState stateQuery) {
        StopWatch stopWatch = new AutomaticStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryStateDAOiBatisImpl SELECT ==> " + stateQuery);
        }
        try {
            return (EntryState) (getSqlMapClientTemplate().queryForObject("selectEntryState", stateQuery));
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.selectEntryState", "", stopWatch);
            }
        }
    }

    //-----------------------
    //       DELETE
    //-----------------------
    /**
     * Delete this Entry DB entry.
     * This form does delete the actual record from the DB.
     */
    public void deleteEntryState(EntryState stateQuery) {
        StopWatch stopWatch = new AutomaticStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryStateDAOiBatisImpl DELETE [ " + stateQuery + " ]");
        }
        try {
            getSqlMapClientTemplate().delete("deleteEntryState", stateQuery);
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.deleteEntryState", "", stopWatch);
            }
        }
    }
}