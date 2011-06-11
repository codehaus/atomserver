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

package org.atomserver.core.dbstore.dao.impl;

import java.util.List;

import org.atomserver.core.PubSubRegistration;
import org.atomserver.core.dbstore.dao.PubSubRegistrationDAO;
import org.atomserver.core.dbstore.dao.impl.rwimpl.AbstractDAOiBatisImpl;
import org.atomserver.core.dbstore.dao.impl.rwimpl.ReadPubSubRegistrationDAOiBatisImpl;
import org.atomserver.core.dbstore.dao.impl.rwimpl.WriteReadPubSubRegistrationDAOiBatisImpl;
import org.springframework.beans.factory.InitializingBean;

/**
 * The original implementation of the CategoryLogEventsDAO,
 * which now delegates to the read-write/read-only Impls.
 * @author Austin Buckner  (abuckner at homeaway.com)
 */
public class PubSubRegistrationDAOiBatisImpl
        extends AbstractDAOiBatisImplDelegator
        implements PubSubRegistrationDAO, InitializingBean {

    private ReadPubSubRegistrationDAOiBatisImpl readDAO;
    private WriteReadPubSubRegistrationDAOiBatisImpl writeReadDAO;

    public void afterPropertiesSet() throws Exception {
        if (dataSource != null) {
            if (writeReadDAO == null) {
                writeReadDAO = new WriteReadPubSubRegistrationDAOiBatisImpl();
                setupDAO(writeReadDAO);
            }
            if (readDAO == null) {
                readDAO = new ReadPubSubRegistrationDAOiBatisImpl();
                setupDAO(readDAO);
            }
        }
    }

    private void setupDAO(ReadPubSubRegistrationDAOiBatisImpl dao) {
        dao.setSqlMapClient(sqlMapClient);
        dao.setDatabaseType(dbType);
        dao.setDataSource(dataSource);
        dao.afterPropertiesSet();
    }

    public ReadPubSubRegistrationDAOiBatisImpl getPubSubRegistrationDAO() { 
        return readDAO; 
    }

    public void setReadPubSubRegistrationDAO(ReadPubSubRegistrationDAOiBatisImpl readDAO) { 
        this.readDAO = readDAO;
    }

    public WriteReadPubSubRegistrationDAOiBatisImpl getWriteReadPubSubRegistrationDAO() { 
        return writeReadDAO; 
    }

    public void setWriteReadPubSubRegistrationDAO(WriteReadPubSubRegistrationDAOiBatisImpl writeReadDAO) {
        this.writeReadDAO = writeReadDAO;
    }

    public AbstractDAOiBatisImpl getReadDAO() { return readDAO; }

    // ----------------------------
    //  ReadEntryCategoryLogEventDAO
    // ----------------------------    

    public PubSubRegistration selectPubSubRegistration(Long regId) {
        return readDAO.selectPubSubRegistration(regId);
    }
    
    public List<PubSubRegistration> selectPubSubRegistrationByCallbackURL(String callbackURL) {
        return readDAO.selectPubSubRegistrationByCallbackURL(callbackURL);
    }
    
    public List<PubSubRegistration> selectPubSubRegistrationByFeedURL(String feedURL) {
        return readDAO.selectPubSubRegistrationByFeedURL(feedURL);
    }
    
    public List<PubSubRegistration> selectAllPubSubRegistrations() {
        return readDAO.selectAllPubSubRegistrations();
    }

    // ----------------------------
    //  WriteReadEntryCategoryLogEventDAO
    // ----------------------------    

    public Long createPubSubRegistration(String feedURL, String callbackURL, Long timestamp) {
        return writeReadDAO.createPubSubRegistration(feedURL, callbackURL, timestamp);
    }
    
    public Long createPubSubRegistration(String feedURL, String callbackURL) {
        return writeReadDAO.createPubSubRegistration(feedURL, callbackURL);
    }
    
    public void updatePubSubRegistration(PubSubRegistration reg) {
        writeReadDAO.updatePubSubRegistration(reg);
    }
    
    public void deletePubSubRegistration(Long regId) {
        writeReadDAO.deletePubSubRegistration(regId);
    }
}
