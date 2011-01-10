/* Copyright (c) 2010 HomeAway, Inc.
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

import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.atomserver.core.PubSubRegistration;



public class PubSubEntryRegistrationDAOTest
        extends DAOTestCase {

    // -------------------------------------------------------
    public static Test suite() { return new TestSuite(PubSubEntryRegistrationDAOTest.class); }

    // -------------------------------------------------------
    protected void setUp() throws Exception {
        super.setUp();
    }

    // -------------------------------------------------------
    protected void tearDown() throws Exception { super.tearDown(); }

    //----------------------------
    //          Tests
    //----------------------------

    public void testCreate() throws Exception {
        String feedURL = "http://??.org";
        String callbackURL = "http://??.com";
        Long timestamp = (long)124;
        Long entryIn= null;
        
        try{
        // INSERT
            entryIn = pubSubRegistrationDAO.createPubSubRegistration(feedURL, callbackURL,timestamp);
            PubSubRegistration entryOut = pubSubRegistrationDAO.selectPubSubRegistration(entryIn);
        
            log.debug("====> entryOut = " + entryOut);
            assertNotNull(entryOut);
        }
        finally{
            // DELETE
            if(entryIn != null){
                pubSubRegistrationDAO.deletePubSubRegistration(entryIn);
                PubSubRegistration entryCheck = pubSubRegistrationDAO.selectPubSubRegistration(entryIn);
                assertNull(entryCheck);
            }
        }
    }
    public void testSelects() throws Exception {
        String feedURL = "http://??.org";
        String callbackURL = "http://??.com";
        String feedURL2 = "http://??.edu";
        String callbackURL2 = "http://??.gov";
        String feedURL3 = "www://??.com";
        String callbackURL3 = "www://??.org";
        Long timestamp = (long)124;
        Long[] group = new Long[5];
        try{
        //INSERT
            group[0] = pubSubRegistrationDAO.createPubSubRegistration(feedURL, callbackURL,timestamp++);
            group[1] = pubSubRegistrationDAO.createPubSubRegistration(feedURL, callbackURL2,timestamp++);
            group[2] = pubSubRegistrationDAO.createPubSubRegistration(feedURL2, callbackURL2,timestamp++);
            group[3] = pubSubRegistrationDAO.createPubSubRegistration(feedURL2, callbackURL,timestamp++);
            group[4] = pubSubRegistrationDAO.createPubSubRegistration(feedURL3, callbackURL3,timestamp);
            
            List <PubSubRegistration> entryOut = pubSubRegistrationDAO.selectPubSubRegistrationByFeedURL(feedURL);
            //test for feedURL select
            log.debug("====> entryOut = " + entryOut);
            assertNotNull(entryOut);
            assertEquals(2, entryOut.size());
            
            entryOut = pubSubRegistrationDAO.selectPubSubRegistrationByCallbackURL(callbackURL);
            //test for callbackURL select
            log.debug("====> entryOut = " + entryOut);
            assertNotNull(entryOut);
            assertEquals(2, entryOut.size());
            
            entryOut = pubSubRegistrationDAO.selectPubSubRegistrationByFeedURL(feedURL3);
            //extra select test using 1 element feedURL
            log.debug("====> entryOut = " + entryOut);
            assertNotNull(entryOut);
            assertEquals(1, entryOut.size());
        }
        finally{
            // DELETE
            for(int i = 0; i< group.length; i++){
                //if(group[i] != null){
                    pubSubRegistrationDAO.deletePubSubRegistration(group[i]);
                    PubSubRegistration entryCheck = pubSubRegistrationDAO.selectPubSubRegistration(group[i]);
                    assertNull(entryCheck);
                //}
            }
        }
        
        
    }
    
    public void testVerifySelectPubSub(){
        String feedURL = "http://??.org";
        String callbackURL = "http://??.com";
        String callbackURL2 = "http://??.edu";
        Long timestamp = (long)124;

        // INSERT
        PubSubRegistration entryIn = new PubSubRegistration();
        entryIn.setFeedURL(feedURL);
        entryIn.setCallbackURL(callbackURL);
        entryIn.setTimestamp(timestamp);

        Long regId = pubSubRegistrationDAO.createPubSubRegistration(feedURL, callbackURL,timestamp);
        Long regId2 = pubSubRegistrationDAO.createPubSubRegistration(feedURL, callbackURL2,timestamp+1);
        PubSubRegistration entryOut = pubSubRegistrationDAO.selectPubSubRegistration(regId);
        
        log.debug("====> entryOut = " + entryOut);
        assertNotNull(entryOut);
        //test for select
        assertEquals(feedURL, entryOut.getFeedURL());
        assertEquals(callbackURL, entryOut.getCallbackURL());
        assertEquals(timestamp, entryOut.getTimestamp());
        
        //test for feedURL
        List <PubSubRegistration> entryList = pubSubRegistrationDAO.selectPubSubRegistrationByFeedURL(feedURL);
        log.debug("====> entryOut = " + entryOut);
        assertNotNull(entryOut);
        assertEquals(2, entryList.size());
        
        // DELETE
        pubSubRegistrationDAO.deletePubSubRegistration(regId);
        PubSubRegistration entryCheck = pubSubRegistrationDAO.selectPubSubRegistration(regId);
        assertNull(entryCheck);
        
        pubSubRegistrationDAO.deletePubSubRegistration(regId2);
        entryCheck = pubSubRegistrationDAO.selectPubSubRegistration(regId2);
        assertNull(entryCheck);
    }
    public void testUpdate(){
        String feedURL = "http://??.org";
        String callbackURL = "http://??.com";
        Long timestamp = (long)124;
        Long newTime = (long)345;
        Long entryIn= null;
        
        try{
        // INSERT
            entryIn = pubSubRegistrationDAO.createPubSubRegistration(feedURL, callbackURL,timestamp);
            PubSubRegistration entryOut = pubSubRegistrationDAO.selectPubSubRegistration(entryIn);
        
            log.debug("====> entryOut = " + entryOut);
            assertNotNull(entryOut);
            
            entryOut.setTimestamp(newTime);
            pubSubRegistrationDAO.updatePubSubRegistration(entryOut);
            PubSubRegistration entryCheck = pubSubRegistrationDAO.selectPubSubRegistration(entryIn);
            assertEquals(newTime, entryCheck.getTimestamp());
        }
        finally{
            // DELETE
            if(entryIn != null){
                pubSubRegistrationDAO.deletePubSubRegistration(entryIn);
                PubSubRegistration entryCheck = pubSubRegistrationDAO.selectPubSubRegistration(entryIn);
                assertNull(entryCheck);
            }
        }
    }
}
