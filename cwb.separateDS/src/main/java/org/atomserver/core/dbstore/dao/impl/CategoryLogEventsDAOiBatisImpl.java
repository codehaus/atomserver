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

package org.atomserver.core.dbstore.dao.impl;

import org.atomserver.EntryDescriptor;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryCategoryLogEvent;
import org.atomserver.core.dbstore.dao.CategoryLogEventsDAO;
import org.atomserver.core.dbstore.dao.impl.rwimpl.AbstractDAOiBatisImpl;
import org.atomserver.core.dbstore.dao.impl.rwimpl.ReadCategoryLogEventsDAOiBatisImpl;
import org.atomserver.core.dbstore.dao.impl.rwimpl.WriteReadCategoryLogEventsDAOiBatisImpl;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;


/**
 * The original implementation of the CategoryLogEventsDAO,
 * which now delegates to the read-write/read-only Impls.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class CategoryLogEventsDAOiBatisImpl
        extends AbstractDAOiBatisImplDelegator
        implements CategoryLogEventsDAO, InitializingBean {

    private ReadCategoryLogEventsDAOiBatisImpl readDAO;
    private WriteReadCategoryLogEventsDAOiBatisImpl writeReadDAO;

    public void afterPropertiesSet() throws Exception {
        if (dataSource != null) {
            if (writeReadDAO == null) {
                writeReadDAO = new WriteReadCategoryLogEventsDAOiBatisImpl();
                setupDAO(writeReadDAO);
            }
            if (readDAO == null) {
                readDAO = new ReadCategoryLogEventsDAOiBatisImpl();
                setupDAO(readDAO);
            }
        }
    }

    private void setupDAO(ReadCategoryLogEventsDAOiBatisImpl dao) {
        dao.setSqlMapClient(sqlMapClient);
        dao.setDatabaseType(dbType);
        dao.setDataSource(dataSource);
        dao.afterPropertiesSet();
    }

    public ReadCategoryLogEventsDAOiBatisImpl getReadEntryCategoryLogEventDAO() { return readDAO; }

    public void setReadEntryCategoryLogEventDAO(ReadCategoryLogEventsDAOiBatisImpl readDAO) { this.readDAO = readDAO; }

    public WriteReadCategoryLogEventsDAOiBatisImpl getWriteReadEntryCategoryLogEventDAO() { return writeReadDAO; }

    public void setWriteReadEntryCategoryLogEventDAO(WriteReadCategoryLogEventsDAOiBatisImpl writeReadDAO) {
        this.writeReadDAO = writeReadDAO;
    }

    public AbstractDAOiBatisImpl getReadDAO() { return readDAO; }

    // ----------------------------
    //  ReadEntryCategoryLogEventDAO
    // ----------------------------    

    public List<EntryCategoryLogEvent> selectEntryCategoryLogEventBySchemeAndTerm(EntryCategory entryQuery) {
        return readDAO.selectEntryCategoryLogEventBySchemeAndTerm(entryQuery);
    }

    public List<EntryCategoryLogEvent> selectEntryCategoryLogEventByScheme(EntryCategory entryQuery) {
        return readDAO.selectEntryCategoryLogEventByScheme(entryQuery);
    }

    public List<EntryCategoryLogEvent> selectEntryCategoryLogEvent(EntryCategory entryQuery) {
        return readDAO.selectEntryCategoryLogEvent(entryQuery);
    }

    public int getTotalCount(String workspace) {return readDAO.getTotalCount(workspace);}

    public int getTotalCount(String workspace, String collection) {return readDAO.getTotalCount(workspace, collection);}


    // ----------------------------
    //  WriteReadEntryCategoryLogEventDAO
    // ----------------------------    

    public int insertEntryCategoryLogEvent(EntryCategory entry) {
        return writeReadDAO.insertEntryCategoryLogEvent(entry);
    }

    public void deleteEntryCategoryLogEventBySchemeAndTerm(EntryCategory entryQuery) {
        writeReadDAO.deleteEntryCategoryLogEventBySchemeAndTerm(entryQuery);
    }

    public void deleteEntryCategoryLogEvent(EntryDescriptor entryQuery) {
        writeReadDAO.deleteEntryCategoryLogEvent(entryQuery);
    }

    public void insertEntryCategoryLogEventBatch(List<EntryCategory> entryCategoryList) {
        writeReadDAO.insertEntryCategoryLogEventBatch(entryCategoryList);
    }

    public void deleteAllEntryCategoryLogEvents(String workspace) {
        writeReadDAO.deleteAllEntryCategoryLogEvents(workspace);
    }

    public void deleteAllEntryCategoryLogEvents(String workspace, String collection) {
        writeReadDAO.deleteAllEntryCategoryLogEvents(workspace, collection);
    }

    public void deleteAllRowsFromEntryCategoryLogEvent() {writeReadDAO.deleteAllRowsFromEntryCategoryLogEvent();}
}
