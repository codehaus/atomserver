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
import org.atomserver.core.dbstore.dao.CategoriesDAO;
import org.atomserver.core.dbstore.dao.impl.rwimpl.AbstractDAOiBatisImpl;
import org.atomserver.core.dbstore.dao.impl.rwimpl.ReadCategoriesDAOiBatisImpl;
import org.atomserver.core.dbstore.dao.impl.rwimpl.WriteReadCategoriesDAOiBatisImpl;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The original implementation of the CategoriesDAO.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class CategoriesDAOiBatisImpl
        extends AbstractDAOiBatisImplDelegator
        implements CategoriesDAO, InitializingBean {

    private ReadCategoriesDAOiBatisImpl readCategoriesDAO;
    private WriteReadCategoriesDAOiBatisImpl writeReadCategoriesDAO;

    public ReadCategoriesDAOiBatisImpl getReadCategoriesDAO() { return readCategoriesDAO; }

    public void setReadCategoriesDAO(ReadCategoriesDAOiBatisImpl readCategoriesDAO) {
        this.readCategoriesDAO = readCategoriesDAO;
    }

    public WriteReadCategoriesDAOiBatisImpl getWriteReadCategoriesDAO() { return writeReadCategoriesDAO; }

    public void setWriteReadCategoriesDAO(WriteReadCategoriesDAOiBatisImpl writeReadCategoriesDAO) {
        this.writeReadCategoriesDAO = writeReadCategoriesDAO;
    }

    public void afterPropertiesSet() throws Exception {
        if (dataSource != null) {
            if (writeReadCategoriesDAO == null) {
                writeReadCategoriesDAO = new WriteReadCategoriesDAOiBatisImpl();
                setupDAO(writeReadCategoriesDAO);
            }
            if (readCategoriesDAO == null) {
                readCategoriesDAO = new ReadCategoriesDAOiBatisImpl();
                setupDAO(readCategoriesDAO);
            }
        }
    }

    private void setupDAO(ReadCategoriesDAOiBatisImpl dao) {
        dao.setSqlMapClient(sqlMapClient);
        dao.setDatabaseType(dbType);
        dao.setDataSource(dataSource);
        dao.afterPropertiesSet();
    }

    public AbstractDAOiBatisImpl getReadDAO() { return readCategoriesDAO; }

    //-------------------
    //   ReadEntryCategoriesDAOiBatisImpl
    //-------------------

    public EntryCategory selectEntryCategory(EntryCategory entryQuery) {
        return readCategoriesDAO.selectEntryCategory(entryQuery);
    }

    public List<EntryCategory> selectEntriesCategories(String workspace, String collection, Set<String> entryIds) {
        return readCategoriesDAO.selectEntriesCategories(workspace, collection, entryIds);
    }

    public List<String> selectDistictCollections(String workspace) {
        return readCategoriesDAO.selectDistictCollections(workspace);
    }

    public List<Map<String, String>> selectDistictCategoriesPerCollection(String workspace, String collection) {
        return readCategoriesDAO.selectDistictCategoriesPerCollection(workspace, collection);
    }

    public int getTotalCount(String workspace) {return readCategoriesDAO.getTotalCount(workspace);}

    public int getTotalCount(String workspace, String collection) {
        return readCategoriesDAO.getTotalCount(workspace, collection);
    }

    //-------------------
    //   WriteReadEntryCategoriesDAOiBatisImpl
    //-------------------
    // These two are used by the AutoTagger
    public List<EntryCategory> selectEntryCategories(EntryDescriptor entryQuery) {
        return writeReadCategoriesDAO.selectEntryCategories(entryQuery);
    }

    public List<EntryCategory> selectEntryCategoriesInScheme(EntryDescriptor entryQuery, String scheme) {
        return writeReadCategoriesDAO.selectEntryCategoriesInScheme(entryQuery, scheme);
    }



    public int insertEntryCategory(EntryCategory entry) {return writeReadCategoriesDAO.insertEntryCategory(entry);}

    public int insertEntryCategoryWithNoCacheUpdate(EntryCategory entry) {
        return writeReadCategoriesDAO.insertEntryCategoryWithNoCacheUpdate(entry);
    }

    public void insertEntryCategoryBatch(List<EntryCategory> entryCategoryList) {
        writeReadCategoriesDAO.insertEntryCategoryBatch(entryCategoryList);
    }

    public void deleteEntryCategory(EntryCategory entryQuery) {
        writeReadCategoriesDAO.deleteEntryCategory(entryQuery);
    }

    public void deleteEntryCategoryBatch(List<EntryCategory> entryCategoryList) {
        writeReadCategoriesDAO.deleteEntryCategoryBatch(entryCategoryList);
    }

    public void deleteEntryCategories(EntryDescriptor entryQuery) {
        writeReadCategoriesDAO.deleteEntryCategories(entryQuery);
    }

    public void deleteEntryCategoriesWithoutCacheUpdate(EntryDescriptor entryQuery) {
        writeReadCategoriesDAO.deleteEntryCategoriesWithoutCacheUpdate(entryQuery);
    }

    public void deleteEntryCategoriesInScheme(EntryMetaData entryQuery, String scheme) {
        writeReadCategoriesDAO.deleteEntryCategoriesInScheme(entryQuery, scheme);
    }

    public int updateEntryCategory(EntryCategory updateCategory, String oldTerm) {
        return writeReadCategoriesDAO.updateEntryCategory(updateCategory, oldTerm);
    }

    public void deleteAllEntryCategories(String workspace) {
        writeReadCategoriesDAO.deleteAllEntryCategories(workspace);
    }

    public void deleteAllEntryCategories(String workspace, String collection) {
        writeReadCategoriesDAO.deleteAllEntryCategories(workspace, collection);
    }

    public void deleteAllRowsFromEntryCategories() {writeReadCategoriesDAO.deleteAllRowsFromEntryCategories();}
}

