/* Copyright Homeaway, Inc 2010. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.rwdao;

import java.util.List;

import org.atomserver.core.PubSubRegistration;
import org.atomserver.core.dbstore.dao.AtomServerDAO;

/**
 * The Read-only DAO for accessing an PubSub Client Feeds
 *
 * @author Austin Buckner  (abuckner at homeaway.com)
 */
public interface ReadPubSubRegistrationDAO extends AtomServerDAO {

    PubSubRegistration selectPubSubRegistration(Long regId);
    
    List<PubSubRegistration> selectPubSubRegistrationByFeedURL(String feedURL);

    List<PubSubRegistration> selectPubSubRegistrationByCallbackURL(String callbackURL);
    
    List<PubSubRegistration> selectAllPubSubRegistrations();
}
