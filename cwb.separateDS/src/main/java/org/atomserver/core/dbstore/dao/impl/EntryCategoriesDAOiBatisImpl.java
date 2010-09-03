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
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.dbstore.dao.EntryCategoriesDAO;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class EntryCategoriesDAOiBatisImpl
        extends AbstractDAOiBatisImplDelegator
        implements EntryCategoriesDAO, InitializingBean {

    private ReadEntryCategoriesDAOiBatisImpl readEntryCategoriesDAO;
    private WriteReadEntryCategoriesDAOiBatisImpl writeReadEntryCategoriesDAO;

    public ReadEntryCategoriesDAOiBatisImpl getReadEntryCategoriesDAO() {
        return readEntryCategoriesDAO;
    }

    public void setReadEntryCategoriesDAO(ReadEntryCategoriesDAOiBatisImpl readEntryCategoriesDAO) {
        this.readEntryCategoriesDAO = readEntryCategoriesDAO;
    }

    public WriteReadEntryCategoriesDAOiBatisImpl getWriteReadEntryCategoriesDAO() {
        return writeReadEntryCategoriesDAO;
    }

    public void setWriteReadEntryCategoriesDAO(WriteReadEntryCategoriesDAOiBatisImpl writeReadEntryCategoriesDAO) {
        this.writeReadEntryCategoriesDAO = writeReadEntryCategoriesDAO;
    }

    public void afterPropertiesSet() throws Exception {
        if (dataSource != null) {
            if (writeReadEntryCategoriesDAO == null) {
                writeReadEntryCategoriesDAO = new WriteReadEntryCategoriesDAOiBatisImpl();
                setupDAO(writeReadEntryCategoriesDAO);
            }
            if (readEntryCategoriesDAO == null) {
                readEntryCategoriesDAO = new ReadEntryCategoriesDAOiBatisImpl();
                setupDAO(readEntryCategoriesDAO);
            }
        }
    }

    private void setupDAO(ReadEntryCategoriesDAOiBatisImpl dao) {
        dao.setSqlMapClient(sqlMapClient);
        dao.setDatabaseType(dbType);
        dao.setDataSource(dataSource);
        dao.afterPropertiesSet();
    }

    public AbstractDAOiBatisImpl getReadDAO() {
        return readEntryCategoriesDAO;
    }

    //-------------------
    //   ReadEntryCategoriesDAOiBatisImpl
    //-------------------

    public EntryCategory selectEntryCategory(EntryCategory entryQuery) {
        return readEntryCategoriesDAO.selectEntryCategory(entryQuery);
    }

    public List<EntryCategory> selectEntriesCategories(String workspace, String collection, Set<String> entryIds) {
        return readEntryCategoriesDAO.selectEntriesCategories(workspace, collection, entryIds);
    }

    public List<EntryCategory> selectEntryCategories(EntryDescriptor entryQuery) {
        return readEntryCategoriesDAO.selectEntryCategories(entryQuery);
    }

    public List<EntryCategory> selectEntryCategoriesInScheme(EntryDescriptor entryQuery, String scheme) {
        return readEntryCategoriesDAO.selectEntryCategoriesInScheme(entryQuery, scheme);
    }

    public List<String> selectDistictCollections(String workspace) {
        return readEntryCategoriesDAO.selectDistictCollections(workspace);
    }

    public List<Map<String, String>> selectDistictCategoriesPerCollection(String workspace, String collection) {
        return readEntryCategoriesDAO.selectDistictCategoriesPerCollection(workspace, collection);
    }

    public int getTotalCount(String workspace) {return readEntryCategoriesDAO.getTotalCount(workspace);}

    public int getTotalCount(String workspace, String collection) {
        return readEntryCategoriesDAO.getTotalCount(workspace, collection);
    }

    //-------------------
    //   WriteReadEntryCategoriesDAOiBatisImpl
    //-------------------

    public int insertEntryCategory(EntryCategory entry) {return writeReadEntryCategoriesDAO.insertEntryCategory(entry);}

    public int insertEntryCategoryWithNoCacheUpdate(EntryCategory entry) {
        return writeReadEntryCategoriesDAO.insertEntryCategoryWithNoCacheUpdate(entry);
    }

    public void insertEntryCategoryBatch(List<EntryCategory> entryCategoryList) {
        writeReadEntryCategoriesDAO.insertEntryCategoryBatch(entryCategoryList);
    }

    public void deleteEntryCategory(EntryCategory entryQuery) {
        writeReadEntryCategoriesDAO.deleteEntryCategory(entryQuery);
    }

    public void deleteEntryCategoryBatch(List<EntryCategory> entryCategoryList) {
        writeReadEntryCategoriesDAO.deleteEntryCategoryBatch(entryCategoryList);
    }

    public void deleteEntryCategories(EntryDescriptor entryQuery) {
        writeReadEntryCategoriesDAO.deleteEntryCategories(entryQuery);
    }

    public void deleteEntryCategoriesWithoutCacheUpdate(EntryDescriptor entryQuery) {
        writeReadEntryCategoriesDAO.deleteEntryCategoriesWithoutCacheUpdate(entryQuery);
    }

    public void deleteEntryCategoriesInScheme(EntryMetaData entryQuery, String scheme) {
        writeReadEntryCategoriesDAO.deleteEntryCategoriesInScheme(entryQuery, scheme);
    }

    public int updateEntryCategory(EntryCategory updateCategory, String oldTerm) {
        return writeReadEntryCategoriesDAO.updateEntryCategory(updateCategory, oldTerm);
    }

    public void deleteAllEntryCategories(String workspace) {
        writeReadEntryCategoriesDAO.deleteAllEntryCategories(workspace);
    }

    public void deleteAllEntryCategories(String workspace, String collection) {
        writeReadEntryCategoriesDAO.deleteAllEntryCategories(workspace, collection);
    }

    public void deleteAllRowsFromEntryCategories() {writeReadEntryCategoriesDAO.deleteAllRowsFromEntryCategories();}
}

