/* Copyright Homeaway, Inc 2010. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.rwdao;

import org.atomserver.core.PubSubRegistration;

/**
 * The read-write DAO for accessing a PubSub Client Feeds
 * This DAO must extend the ReadPubSubRegistrationDAO because the POST,PUT,DELETE sequence
 * will need to perform read queries as well as write queries.
 * @author Austin Buckner  (abuckner at homeaway.com)
 */
public interface WriteReadPubSubRegistrationDAO extends ReadPubSubRegistrationDAO {

    Long createPubSubRegistration(String feedURL, String callbackURL);
    
    Long createPubSubRegistration(String feedURL, String callbackURL, Long timestamp);
    
    void updatePubSubRegistration(PubSubRegistration reg);

    void deletePubSubRegistration(Long regId);
}