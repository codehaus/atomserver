/* Copyright Homeaway, Inc 2010. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.impl.rwimpl;

import org.atomserver.core.PubSubRegistration;
import org.atomserver.core.dbstore.dao.rwdao.WriteReadPubSubRegistrationDAO;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;

/**
 * @author Austin Buckner  (abuckner at homeaway.com)
 */
public class WriteReadPubSubRegistrationDAOiBatisImpl
        extends ReadPubSubRegistrationDAOiBatisImpl
        implements WriteReadPubSubRegistrationDAO {

    //======================================
    //           CRUD methods
    //======================================
    
    /**
     *  Create a single PubSubRegistration
     */
    public Long createPubSubRegistration(String feedURL, String callbackURL) {
        return createPubSubRegistration(feedURL, callbackURL, null);
    }
    public Long createPubSubRegistration(String feedURL, String callbackURL, Long timestamp) {
        StopWatch stopWatch = new AtomServerStopWatch();
        
        PubSubRegistration reg = new PubSubRegistration();
        reg.setFeedURL(feedURL);
        reg.setCallbackURL(callbackURL);
        reg.setTimestamp(timestamp == null ? 0L : timestamp);
            
        if (log.isDebugEnabled()) {
                log.debug("PubSubRegistrationDAOiBatisImpl CREATE ==> " + reg);
        }
        try {
           // ParamMap paramMap = prepareInsertParamMap(reg);
            return (Long) getSqlMapClientTemplate().insert("createPubSubRegistration-"+ getDatabaseType(), reg);
             
        }
        finally {
            stopWatch.stop("DB.createPubSubRegistration",
                            AtomServerPerfLogTagFormatter.getPerfLogEntryPubSubString(reg));
        }
    }
    ParamMap prepareParamMap(PubSubRegistration reg) {
        ParamMap paramMap = paramMap()
                .param("feedURL", reg.getFeedURL())
                .param("callbackURL", reg.getCallbackURL())
                .param("timestamp", reg.getTimestamp());

        if (log.isDebugEnabled()) {
            log.debug("PubSubRegistrationDAOiBatisImpl UPDATE:: paramMap= " + paramMap);
        }
        return paramMap;
    }
    
    /**
     *  Update a single PubSubRegistration   <timestamp>
     */
    public void updatePubSubRegistration(PubSubRegistration reg) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("PubSubRegistrationDAOiBatisImpl UPDATE [ " + reg + " ]");
        }
        try {
            //ParamMap paramMap = prepareParamMap(reg);
            getSqlMapClientTemplate().update("updatePubSubRegistration", reg);
        }
        finally {
            stopWatch.stop("DB.updatePubSubRegistration", 
                           AtomServerPerfLogTagFormatter.getPerfLogEntryPubSubString(reg));

        }
    }

    /**
     *  Delete a single PubSubRegistration
     */
    public void deletePubSubRegistration(Long regId) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("PubSubRegistrationDAOiBatisImpl DELETE [ " + regId + " ]");
        }
        try {
            //ParamMap paramMap = prepareUpdateParamMap(reg);
            getSqlMapClientTemplate().delete("deletePubSubRegistration", regId);
        }
        finally {
            stopWatch.stop("DB.deletePubSubRegistration", "Delete");

        }
    }
}
