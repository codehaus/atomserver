/* Copyright Homeaway, Inc 2010. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.impl.rwimpl;

import java.util.List;

import org.atomserver.core.PubSubRegistration;
import org.atomserver.core.dbstore.dao.rwdao.ReadPubSubRegistrationDAO;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;

/**
 * @author Austin Buckner  (abuckner at homeaway.com)
 */
public class ReadPubSubRegistrationDAOiBatisImpl
        extends AbstractDAOiBatisImpl
        implements ReadPubSubRegistrationDAO {

    /**
     *  Select a single PubSubRegistration
     */
    public PubSubRegistration selectPubSubRegistration(Long regId) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("PubSubRegistrationDAOiBatisImpl SELECT ==> " + regId);
        }
        try {
            return (PubSubRegistration) getSqlMapClientTemplate().queryForObject("selectPubSubRegistration", regId);
        }
        finally {
            stopWatch.stop("DB.selectPubSubRegistration","["+ regId + "]");
        }
    }

    /**
     *  Select a single PubSubRegistration from feedURL
     */
    @SuppressWarnings("unchecked") //For the List cast
    public List<PubSubRegistration> selectPubSubRegistrationByFeedURL(String feedURL) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("PubSubRegistrationDAOiBatisImpl SELECT ==> " + feedURL);
        }
        try {
            return (List<PubSubRegistration>)
                    (getSqlMapClientTemplate().queryForList("selectPubSubRegistrationByFeedURL", feedURL));
        }
        finally {
            stopWatch.stop("DB.selectPubSubRegistrationByFeedURL","["+feedURL+"]");
        }
    }
    
    /**
     *  Select a single PubSubRegistration from callbackURL
     */
    @SuppressWarnings("unchecked") //For the List cast
    public List<PubSubRegistration> selectPubSubRegistrationByCallbackURL(String callbackURL) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("PubSubRegistrationDAOiBatisImpl SELECT ==> " + callbackURL);
        }
        try {
            return (List<PubSubRegistration>)
                    (getSqlMapClientTemplate().queryForList("selectPubSubRegistrationByCallbackURL", callbackURL));
        }
        finally {
            stopWatch.stop("DB.selectPubSubRegistrationByFeedURL","["+callbackURL+"]");
        }
    }
    
    /**
     *  Select all PubSubRegistrations
     */
    @SuppressWarnings("unchecked") //For the List cast
    public List<PubSubRegistration> selectAllPubSubRegistrations() {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("PubSubRegistrationDAOiBatisImpl SELECT ==> *");
        }
        try {
            return (List<PubSubRegistration>)
                    (getSqlMapClientTemplate().queryForList("selectAllPubSubRegistration"));
        }
        finally {
            stopWatch.stop("DB.selectAllPubSubRegistration","Select All");
        }
    }


}
